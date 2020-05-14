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
import esw.sm.api.models.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.models.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.api.models._
import esw.sm.impl.utils.SequencerUtil

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequenceManagerBehaviorTest extends ScalaTestWithActorTestKit with BaseTestSuite {

  private val Darknight                        = "darknight"
  private val Clearskies                       = "clearskies"
  private val RandomObsMode                    = "RandomObsMode"
  private val darknightSequencers: Sequencers  = Sequencers(ESW, TCS)
  private val clearskiesSequencers: Sequencers = Sequencers(ESW)
  private val config = SequenceManagerConfig(
    Map(
      Darknight  -> ObsModeConfig(Resources("r1", "r2"), darknightSequencers),
      Clearskies -> ObsModeConfig(Resources("r2", "r3"), clearskiesSequencers)
    )
  )
  private val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil             = mock[SequencerUtil]
  private val sequenceManagerBehavior = new SequenceManagerBehavior(
    config,
    locationServiceUtil,
    sequencerUtil
  )

  private lazy val smRef: ActorRef[SequenceManagerMsg] = spawn(sequenceManagerBehavior.idle(), "test_actor")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override protected def afterEach(): Unit = reset(locationServiceUtil, sequencerUtil)

  "Configure" must {

    "transition sm from IDLE -> ConfigurationInProcess -> Idle state and return location of master sequencer | ESW-178, ESW-164" in {
      val httpLocation   = HttpLocation(HttpConnection(ComponentId(Prefix(ESW, Darknight), Sequencer)), new URI("uri"))
      val configResponse = Success(httpLocation)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(Darknight, darknightSequencers)).thenReturn(Future.successful(configResponse))
      val configureProbe = createTestProbe[ConfigureResponse]

      // STATE TRANSITION: IDLE -> Configure() -> ConfigurationInProcess -> Idle
      assertState(Idle)
      smRef ! Configure(Darknight, configureProbe.ref)
      assertState(ConfigurationInProcess)
      assertState(Idle)

      configureProbe.expectMessage(configResponse)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(Darknight, darknightSequencers)
    }

    "return LocationServiceError if location service fails to return running observation mode | ESW-178" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("Sequencer"))))

      val probe = createTestProbe[ConfigureResponse]
      smRef ! Configure(Darknight, probe.ref)

      probe.expectMessage(LocationServiceError("Sequencer"))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return ConflictingResourcesWithRunningObsMode when required resources are already in use | ESW-169" in {
      // this simulates that Clearskies observation is running
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, Clearskies), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = createTestProbe[ConfigureResponse]

      // r2 is a conflicting resource between Darknight and Clearskies observations
      smRef ! Configure(Darknight, probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode(Set(Clearskies)))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(Darknight, darknightSequencers)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-164" in {
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, RandomObsMode), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = createTestProbe[ConfigureResponse]

      smRef ! Configure(RandomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(RandomObsMode))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }
  }

  "Cleanup" must {

    "transition sm from IDLE -> CleaningInProcess -> Idle state and stop all the sequencer for given obs mode | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, Darknight)).thenReturn(future(1.seconds, Right(Done)))

      val cleanupProbe = createTestProbe[CleanupResponse]

      assertState(Idle)
      smRef ! Cleanup(Darknight, cleanupProbe.ref)
      assertState(CleaningInProcess)
      assertState(Idle)

      cleanupProbe.expectMessage(CleanupResponse.Success)
      verify(sequencerUtil).stopSequencers(darknightSequencers, Darknight)
    }

    "return fail if there is failure while stopping sequencers | ESW-166" in {
      val failureMsg = "location service error"
      when(sequencerUtil.stopSequencers(darknightSequencers, Darknight))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(failureMsg))))

      val probe = createTestProbe[CleanupResponse]
      smRef ! Cleanup(Darknight, probe.ref)

      probe.expectMessage(LocationServiceError(failureMsg))
      verify(sequencerUtil).stopSequencers(darknightSequencers, Darknight)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-166" in {
      val probe = createTestProbe[CleanupResponse]
      smRef ! Cleanup(RandomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(RandomObsMode))
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
