package esw.sm.impl.core

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.commons.{BaseTestSuite, Timeouts}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg.{Cleanup, Configure}
import esw.sm.api.models.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, LocationServiceError, Success}
import esw.sm.api.models._
import esw.sm.impl.utils.SequencerUtil

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequenceManagerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  val DARKNIGHT                                = "darknight"
  val CLEARSKIES                               = "clearskies"
  private val darknightSequencers: Sequencers  = Sequencers(ESW, TCS)
  private val clearskiesSequencers: Sequencers = Sequencers(ESW)
  private val config = Map(
    DARKNIGHT  -> ObsModeConfig(Resources("r1", "r2"), darknightSequencers),
    CLEARSKIES -> ObsModeConfig(Resources("r2", "r3"), clearskiesSequencers)
  )
  private val locationServiceUtil: LocationServiceUtil =
    mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil = mock[SequencerUtil]
  private val sequenceManagerBehavior =
    new SequenceManagerBehavior(config, locationServiceUtil, sequencerUtil)

  private val smRef: ActorRef[SequenceManagerMsg] =
    system.systemActorOf(sequenceManagerBehavior.idle(), "test_actor")

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(locationServiceUtil, sequencerUtil)
  }

  "Configure" must {

    "show that cleanup and configuration can not be done when ConfigurationInProgress | ESW-178" in {
      val httpLocation = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(DARKNIGHT, darknightSequencers))
        .thenReturn(Future.successful(Success(httpLocation)))
      val configureProbe1 = createTestProbe[ConfigureResponse]
      val configureProbe2 = createTestProbe[ConfigureResponse]
      val cleanupProbe    = createTestProbe[CleanupResponse]

      // parallel cleanup and configure
      smRef ! Configure(DARKNIGHT, configureProbe1.ref)
      smRef ! Cleanup(DARKNIGHT, cleanupProbe.ref)
      smRef ! Configure(CLEARSKIES, configureProbe2.ref)

      // verify success
      configureProbe1.expectMessage(Timeouts.DefaultTimeout, ConfigureResponse.Success(httpLocation))
      // verify that configure and cleanup can not be while configuration is in progress
      cleanupProbe.expectNoMessage
      configureProbe2.expectNoMessage
    }

    "start sequence hierarchy and return master sequencer | ESW-178, ESW-164" in {
      val httpLocation = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Right(List.empty)))
      when(sequencerUtil.startSequencers(DARKNIGHT, darknightSequencers))
        .thenReturn(Future.successful(Success(httpLocation)))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure(DARKNIGHT, probe.ref)

      probe.expectMessage(Success(httpLocation))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(DARKNIGHT, darknightSequencers)
    }

    "return ConfigurationFailure if location service fails to return running observation mode | ESW-178" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("Sequencer"))))

      val probe = createTestProbe[ConfigureResponse]
      smRef ! Configure(DARKNIGHT, probe.ref)

      probe.expectMessage(LocationServiceError("Sequencer"))

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return ConflictingResourcesWithRunningObsMode when required resources are already in use | ESW-169" in {
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, CLEARSKIES), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure("darknight", probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode(Set(CLEARSKIES)))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(DARKNIGHT, darknightSequencers)
    }
  }

  "Cleanup" must {

    "show that cleanup and configuration can not be done when CleanupInProgress | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, DARKNIGHT))
        .thenReturn(future(1.seconds, Right(Done)))

      val cleanupProbe1  = createTestProbe[CleanupResponse]
      val cleanupProbe2  = createTestProbe[CleanupResponse]
      val configureProbe = createTestProbe[ConfigureResponse]

      // parallel cleanup and configure
      smRef ! Cleanup(DARKNIGHT, cleanupProbe1.ref)
      smRef ! Cleanup(CLEARSKIES, cleanupProbe2.ref)
      smRef ! Configure(CLEARSKIES, configureProbe.ref)

      // verify success
      cleanupProbe1.expectMessage(CleanupResponse.Success)

      // verify that configure and cleanup can not be while configuration is in progress
      cleanupProbe2.expectNoMessage
      configureProbe.expectNoMessage
    }

    "stop all the sequencers of the given observation mode | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, DARKNIGHT))
        .thenReturn(Future.successful(Right(Done)))

      val probe = createTestProbe[CleanupResponse]
      smRef ! Cleanup(DARKNIGHT, probe.ref)

      probe.expectMessage(CleanupResponse.Success)
      verify(sequencerUtil).stopSequencers(darknightSequencers, DARKNIGHT)
    }

    "return fail if there is failure while stopping sequencers | ESW-166" in {
      val failureMsg = "location service error"
      when(sequencerUtil.stopSequencers(darknightSequencers, DARKNIGHT))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(failureMsg))))

      val probe = createTestProbe[CleanupResponse]
      smRef ! Cleanup(DARKNIGHT, probe.ref)

      probe.expectMessage(CleanupResponse.Failed(failureMsg))
      verify(sequencerUtil).stopSequencers(darknightSequencers, DARKNIGHT)
    }
  }
}
