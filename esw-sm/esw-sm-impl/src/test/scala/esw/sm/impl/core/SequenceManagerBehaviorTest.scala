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
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningInProcess, ConfigurationInProcess, Idle}
import esw.sm.api.models.ConfigureResponse.{ConfigurationFailure, ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.api.actor.messages.SequenceManagerMsg.{Cleanup, Configure, GetSequenceManagerState}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.models.SequenceManagerError.SequencerNotIdle
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, ObsModeConfig, Resources, Sequencers}
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
  private val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil             = mock[SequencerUtil]
  private val sequenceManagerBehavior                  = new SequenceManagerBehavior(config, locationServiceUtil, sequencerUtil)

  private val smRef: ActorRef[SequenceManagerMsg] = system.systemActorOf(sequenceManagerBehavior.init(), "test_actor")

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(locationServiceUtil, sequencerUtil)
  }

  "Configure" must {

    "transition sequence manager to ConfigurationInProcess state | ESW-178" in {
      val httpLocation = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(DARKNIGHT, darknightSequencers)).thenReturn(Future.successful(Success(httpLocation)))
      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(Left(ResolveLocationFailed("error"))))
      val configureProbe = createTestProbe[ConfigureResponse]

      smRef ! Configure(DARKNIGHT, configureProbe.ref)

      val stateProbe = TestProbe[SequenceManagerState]
      eventually {
        smRef ! GetSequenceManagerState(stateProbe.ref)
        stateProbe.expectMessage(ConfigurationInProcess)
      }
      eventually {
        smRef ! GetSequenceManagerState(stateProbe.ref)
        stateProbe.expectMessage(Idle)
      }
    }

    "start sequence hierarchy and return master sequencer | ESW-178" in {
      val httpLocation = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List.empty)))
      when(sequencerUtil.startSequencers(DARKNIGHT, darknightSequencers)).thenReturn(Future.successful(Success(httpLocation)))
      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(Left(ResolveLocationFailed("error"))))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure(DARKNIGHT, probe.ref)

      probe.expectMessage(Success(httpLocation))
      verify(sequencerUtil).resolveMasterSequencerOf(DARKNIGHT)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(DARKNIGHT, darknightSequencers)
    }

    "return ConfigurationFailure if location service fails to return running observation mode | ESW-178" in {
      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(Left(ResolveLocationFailed("error"))))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("Sequencer"))))

      val probe = createTestProbe[ConfigureResponse]
      smRef ! Configure(DARKNIGHT, probe.ref)

      probe.expectMessage(ConfigurationFailure("Sequencer"))

      verify(sequencerUtil).resolveMasterSequencerOf(DARKNIGHT)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return ConflictingResourcesWithRunningObsMode when required resources are already in use | ESW-178" in {
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, CLEARSKIES), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(Left(ResolveLocationFailed("error"))))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure("darknight", probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode)
      verify(sequencerUtil).resolveMasterSequencerOf(DARKNIGHT)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(DARKNIGHT, darknightSequencers)
    }

    "return location of already spawned Sequencer Hierarchy if all the sequencers are Idle | ESW-178" in {
      val masterLoc = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, CLEARSKIES), Sequencer)), new URI("uri"))

      when(sequencerUtil.resolveMasterSequencerOf(CLEARSKIES)).thenReturn(Future.successful(Right(masterLoc)))
      when(sequencerUtil.checkForSequencersAvailability(clearskiesSequencers, CLEARSKIES))
        .thenReturn(Future.successful(Right(Done)))

      val probe = createTestProbe[ConfigureResponse]
      smRef ! Configure(CLEARSKIES, probe.ref)

      probe.expectMessage(Success(masterLoc))

      verify(sequencerUtil).resolveMasterSequencerOf(CLEARSKIES)
      verify(sequencerUtil).checkForSequencersAvailability(clearskiesSequencers, CLEARSKIES)
    }

    "return ConfigurationFailure if sequencer hierarchy already spawned and the any of the sequencer is not Idle | ESW-178" in {
      val masterLoc = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, DARKNIGHT), Sequencer)), new URI("uri"))

      when(sequencerUtil.resolveMasterSequencerOf(DARKNIGHT)).thenReturn(Future.successful(Right(masterLoc)))
      when(sequencerUtil.checkForSequencersAvailability(darknightSequencers, DARKNIGHT))
        .thenReturn(Future.successful(Left(SequencerNotIdle(DARKNIGHT)))) // mimics that one or more sequencers are not Idle)

      val probe = createTestProbe[ConfigureResponse]
      smRef ! Configure(DARKNIGHT, probe.ref)

      probe.expectMessage(ConfigurationFailure(s"Sequencers for $DARKNIGHT are already executing another sequence"))

      verify(sequencerUtil).resolveMasterSequencerOf(DARKNIGHT)
      verify(sequencerUtil).checkForSequencersAvailability(darknightSequencers, DARKNIGHT)
    }
  }

  "Cleanup" must {

    "transition sequence manager to CleaningInProcess state | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, DARKNIGHT)).thenReturn(future(1.seconds, Right(Done)))

      val cleanupProbe = createTestProbe[CleanupResponse]
      smRef ! Cleanup(DARKNIGHT, cleanupProbe.ref)

      val stateProbe = TestProbe[SequenceManagerState]
      eventually {
        smRef ! GetSequenceManagerState(stateProbe.ref)
        stateProbe.expectMessage(CleaningInProcess)
      }
      eventually {
        smRef ! GetSequenceManagerState(stateProbe.ref)
        stateProbe.expectMessage(Idle)
      }
    }

    "stop all the sequencers of the given observation mode | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, DARKNIGHT)).thenReturn(Future.successful(Right(Done)))

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
