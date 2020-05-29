package esw.sm.impl.core

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.BaseTestSuite
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningUp, Configuring, Idle, StartingSequencer}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg.{Cleanup, Configure, GetSequenceManagerState, StartSequencer}
import esw.sm.api.models.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.models.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.api.models.SequenceManagerError.LoadScriptError
import esw.sm.api.models._
import esw.sm.impl.config.{ObsModeConfig, Resources, SequenceManagerConfig, Sequencers}
import esw.sm.impl.utils.SequencerUtil

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequenceManagerBehaviorTest extends BaseTestSuite {

  private implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(SpawnProtocol(), "sequence-manager-system")

  private val Darknight                        = "darknight"
  private val Clearskies                       = "clearskies"
  private val RandomObsMode                    = "RandomObsMode"
  private val darknightSequencers: Sequencers  = Sequencers(ESW, TCS)
  private val clearskiesSequencers: Sequencers = Sequencers(ESW)
  private val config = SequenceManagerConfig(
    Map(
      Darknight  -> ObsModeConfig(Resources("r1", "r2"), darknightSequencers),
      Clearskies -> ObsModeConfig(Resources("r2", "r3"), clearskiesSequencers)
    ),
    sequencerStartRetries = 3
  )
  private val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil             = mock[SequencerUtil]
  private val sequenceManagerBehavior = new SequenceManagerBehavior(
    config,
    locationServiceUtil,
    sequencerUtil
  )

  private lazy val smRef: ActorRef[SequenceManagerMsg] = actorSystem.systemActorOf(sequenceManagerBehavior.idle(), "test_actor")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override protected def beforeEach(): Unit = reset(locationServiceUtil, sequencerUtil)

  "Configure" must {

    "transition sm from Idle -> ConfigurationInProcess -> Idle state and return location of master sequencer | ESW-178, ESW-164" in {
      val componentId    = ComponentId(Prefix(ESW, Darknight), Sequencer)
      val configResponse = Success(componentId)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(Darknight, darknightSequencers, 3)).thenReturn(Future.successful(configResponse))
      val configureProbe = TestProbe[ConfigureResponse]()

      // STATE TRANSITION: Idle -> Configure() -> ConfigurationInProcess -> Idle
      assertState(Idle)
      smRef ! Configure(Darknight, configureProbe.ref)
      assertState(Configuring)
      assertState(Idle)

      configureProbe.expectMessage(configResponse)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(Darknight, darknightSequencers, 3)
    }

    "return LocationServiceError if location service fails to return running observation mode | ESW-178" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("Sequencer"))))

      val probe = TestProbe[ConfigureResponse]()
      smRef ! Configure(Darknight, probe.ref)

      probe.expectMessage(LocationServiceError("Sequencer"))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return ConflictingResourcesWithRunningObsMode when required resources are already in use | ESW-169" in {
      // this simulates that Clearskies observation is running
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, Clearskies), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = TestProbe[ConfigureResponse]()

      // r2 is a conflicting resource between Darknight and Clearskies observations
      smRef ! Configure(Darknight, probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode(Set(Clearskies)))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(Darknight, darknightSequencers, 3)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-164" in {
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, RandomObsMode), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = TestProbe[ConfigureResponse]()

      smRef ! Configure(RandomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(RandomObsMode))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }
  }

  "Cleanup" must {

    "transition sm from Idle -> CleaningInProcess -> Idle state and stop all the sequencer for given obs mode | ESW-166" in {
      when(sequencerUtil.stopSequencers(darknightSequencers, Darknight)).thenReturn(future(1.seconds, Right(Done)))

      val cleanupProbe = TestProbe[CleanupResponse]()

      assertState(Idle)
      smRef ! Cleanup(Darknight, cleanupProbe.ref)
      assertState(CleaningUp)
      assertState(Idle)

      cleanupProbe.expectMessage(CleanupResponse.Success)
      verify(sequencerUtil).stopSequencers(darknightSequencers, Darknight)
    }

    "return fail if there is failure while stopping sequencers | ESW-166" in {
      val failureMsg = "location service error"
      when(sequencerUtil.stopSequencers(darknightSequencers, Darknight))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(failureMsg))))

      val probe = TestProbe[CleanupResponse]()
      smRef ! Cleanup(Darknight, probe.ref)

      probe.expectMessage(LocationServiceError(failureMsg))
      verify(sequencerUtil).stopSequencers(darknightSequencers, Darknight)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-166" in {
      val probe = TestProbe[CleanupResponse]()
      smRef ! Cleanup(RandomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(RandomObsMode))
    }
  }

  "StartSequencer" must {
    "transition sm from Idle -> Starting -> Idle state and start the sequencer for given obs mode | ESW-166" in {
      val componentId    = ComponentId(Prefix(ESW, Darknight), Sequencer)
      val httpConnection = HttpConnection(componentId)
      val akkaLocation   = AkkaLocation(AkkaConnection(componentId), new URI("uri"))

      when(sequencerUtil.startSequencer(ESW, Darknight, 3)).thenReturn(future(1.seconds, Right(akkaLocation)))
      when(locationServiceUtil.find(httpConnection)).thenReturn(futureLeft(LocationNotFound("error")))

      val startSequencerResponseProbe = TestProbe[StartSequencerResponse]()

      assertState(Idle)
      smRef ! StartSequencer(ESW, Darknight, startSequencerResponseProbe.ref)
      assertState(StartingSequencer)
      startSequencerResponseProbe.expectMessage(StartSequencerResponse.Started(componentId))
      assertState(Idle)

      verify(sequencerUtil).startSequencer(ESW, Darknight, 3)
      verify(locationServiceUtil).find(httpConnection)
    }

    "return AlreadyRunning if sequencer for given obs mode is already running | ESW-166" in {
      val componentId    = ComponentId(Prefix(ESW, Darknight), Sequencer)
      val httpConnection = HttpConnection(componentId)
      val httpLocation   = HttpLocation(httpConnection, new URI("uri"))

      when(locationServiceUtil.find(httpConnection))
        .thenReturn(futureRight(httpLocation))

      val startSequencerResponseProbe = TestProbe[StartSequencerResponse]()

      smRef ! StartSequencer(ESW, Darknight, startSequencerResponseProbe.ref)

      startSequencerResponseProbe.expectMessage(StartSequencerResponse.AlreadyRunning(componentId))
      verify(sequencerUtil, never).startSequencer(ESW, Darknight, 3)
      verify(locationServiceUtil).find(httpConnection)
    }

    "return Error if start sequencer returns error | ESW-166" in {
      val componentId           = ComponentId(Prefix(ESW, Darknight), Sequencer)
      val httpConnection        = HttpConnection(componentId)
      val expectedErrorResponse = LoadScriptError("error")

      when(locationServiceUtil.find(httpConnection))
        .thenReturn(futureLeft(LocationNotFound("error")))
      when(sequencerUtil.startSequencer(ESW, Darknight, 3)).thenReturn(futureLeft(expectedErrorResponse))

      val startSequencerResponseProbe = TestProbe[StartSequencerResponse]()

      smRef ! StartSequencer(ESW, Darknight, startSequencerResponseProbe.ref)

      startSequencerResponseProbe.expectMessage(expectedErrorResponse)
      verify(sequencerUtil).startSequencer(ESW, Darknight, 3)
      verify(locationServiceUtil).find(httpConnection)
    }
  }

  private def assertState(state: SequenceManagerState) = {
    val stateProbe = TestProbe[SequenceManagerState]()
    eventually {
      smRef ! GetSequenceManagerState(stateProbe.ref)
      stateProbe.expectMessage(state)
    }
  }
}
