package esw.sm.impl.core

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.ActorRef
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.BaseTestSuite
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningInProcess, ConfigurationInProcess, Idle}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg.{Cleanup, Configure, GetSequenceManagerState}
import esw.sm.api.models.ConfigureResponse.{LocationServiceError, ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.api.models._
import esw.sm.impl.utils.SequencerUtil

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequenceManagerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val DARKNIGHT                        = "darknight"
  private val CLEARSKIES                       = "clearskies"
  private val darknightSequencers: Sequencers  = Sequencers(ESW, TCS)
  private val clearskiesSequencers: Sequencers = Sequencers(ESW)
  private val config = Map(
    DARKNIGHT  -> ObsModeConfig(Resources("r1", "r2"), darknightSequencers),
    CLEARSKIES -> ObsModeConfig(Resources("r2", "r3"), clearskiesSequencers)
  )
  private val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil             = mock[SequencerUtil]
  private val sequenceManagerBehavior                  = new SequenceManagerBehavior(config, locationServiceUtil, sequencerUtil)

  private lazy val smRef: ActorRef[SequenceManagerMsg] = system.systemActorOf(sequenceManagerBehavior.idle(), "test_actor")

  override protected def afterEach(): Unit = reset(locationServiceUtil, sequencerUtil)

  "Configure" must {

    "transition sm from IDLE -> ConfigurationInProcess -> Idle state and return location of master sequencer| ESW-178, ESW-164" in {
      val httpLocation   = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))
      val configResponse = Success(httpLocation)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(DARKNIGHT, darknightSequencers)).thenReturn(Future.successful(configResponse))
      val configureProbe = createTestProbe[ConfigureResponse]

      // STATE TRANSITION: IDLE -> Configure() -> ConfigurationInProcess -> Idle
      assertState(Idle)
      smRef ! Configure(DARKNIGHT, configureProbe.ref)
      assertState(ConfigurationInProcess)
      assertState(Idle)

      configureProbe.expectMessage(configResponse)
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

    "transition sequence manager to CleaningInProcess state | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, DARKNIGHT))
        .thenReturn(future(1.seconds, Right(Done)))

      val cleanupProbe = createTestProbe[CleanupResponse]
      smRef ! Cleanup(DARKNIGHT, cleanupProbe.ref)

      assertState(CleaningInProcess)
      assertState(Idle)
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

  private def assertState(state: SequenceManagerState) = {
    val stateProbe = TestProbe[SequenceManagerState]
    eventually {
      smRef ! GetSequenceManagerState(stateProbe.ref)
      stateProbe.expectMessage(state)
    }
  }
}
