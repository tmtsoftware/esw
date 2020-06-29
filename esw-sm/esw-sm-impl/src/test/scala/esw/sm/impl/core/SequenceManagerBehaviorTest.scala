package esw.sm.impl.core

import java.net.URI

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem._
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState._
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.protocol.AgentError.SpawnSequenceComponentFailed
import esw.sm.api.protocol.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.protocol.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.api.protocol.ShutdownAllSequencersResponse.ShutdownFailure
import esw.sm.api.protocol.ShutdownSequenceComponentResponse.ShutdownSequenceComponentFailure
import esw.sm.api.protocol.ShutdownSequencerResponse.UnloadScriptError
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.api.protocol.{ShutdownSequenceComponentResponse, _}
import esw.sm.impl.config._
import esw.sm.impl.utils.{SequenceComponentUtil, SequencerUtil}
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequenceManagerBehaviorTest extends BaseTestSuite with TableDrivenPropertyChecks {

  private implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(SpawnProtocol(), "sequence-manager-system")

  private val Darknight                        = ObsMode("darknight")
  private val Clearskies                       = ObsMode("clearskies")
  private val RandomObsMode                    = ObsMode("RandomObsMode")
  private val darknightSequencers: Sequencers  = Sequencers(ESW, TCS)
  private val clearskiesSequencers: Sequencers = Sequencers(ESW)
  private val config = SequenceManagerConfig(
    Map(
      Darknight  -> ObsModeConfig(Resources(Resource(NSCU), Resource(TCS)), darknightSequencers),
      Clearskies -> ObsModeConfig(Resources(Resource(TCS), Resource(IRIS)), clearskiesSequencers)
    ),
    sequencerStartRetries = 3
  )
  private val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
  private val sequencerUtil: SequencerUtil                 = mock[SequencerUtil]
  private val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
  private val sequenceManagerBehavior = new SequenceManagerBehavior(
    config,
    locationServiceUtil,
    sequencerUtil,
    sequenceComponentUtil
  )

  private lazy val smRef: ActorRef[SequenceManagerMsg] = actorSystem.systemActorOf(sequenceManagerBehavior.setup, "test_actor")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override protected def beforeEach(): Unit = reset(locationServiceUtil, sequencerUtil, sequenceComponentUtil)

  "Configure" must {

    "transition sm from Idle -> ConfigurationInProcess -> Idle state and return location of master sequencer | ESW-178, ESW-164" in {
      val componentId    = ComponentId(Prefix(ESW, Darknight.name), Sequencer)
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

    "return ConflictingResourcesWithRunningObsMode when required resources are already in use | ESW-169, ESW-168, ESW-170, ESW-179" in {
      // this simulates that Clearskies observation is running
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, Clearskies.name), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = TestProbe[ConfigureResponse]()

      // r2 is a conflicting resource between Darknight and Clearskies observations
      smRef ! Configure(Darknight, probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode(Set(Clearskies)))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(Darknight, darknightSequencers, 3)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-164" in {
      val akkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, RandomObsMode.name), Sequencer)), new URI("uri"))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(akkaLocation))))
      val probe = TestProbe[ConfigureResponse]()

      smRef ! Configure(RandomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(RandomObsMode))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }
  }

  "Cleanup" must {

    "transition sm from Idle -> CleaningInProcess -> Idle state and stop all the sequencer for given obs mode | ESW-166" in {
      when(sequencerUtil.shutdownSequencers(darknightSequencers, Darknight))
        .thenReturn(future(1.seconds, CleanupResponse.Success))

      val cleanupProbe = TestProbe[CleanupResponse]()

      assertState(Idle)
      smRef ! Cleanup(Darknight, cleanupProbe.ref)
      assertState(CleaningUp)
      assertState(Idle)

      cleanupProbe.expectMessage(CleanupResponse.Success)
      verify(sequencerUtil).shutdownSequencers(darknightSequencers, Darknight)
    }

    "return fail if there is failure while stopping sequencers | ESW-166" in {
      val failureMsg = "location service error"
      when(sequencerUtil.shutdownSequencers(darknightSequencers, Darknight))
        .thenReturn(Future.successful(CleanupResponse.FailedToShutdownSequencers(Set(failureMsg))))

      val probe = TestProbe[CleanupResponse]()
      smRef ! Cleanup(Darknight, probe.ref)

      probe.expectMessage(CleanupResponse.FailedToShutdownSequencers(Set(failureMsg)))
      verify(sequencerUtil).shutdownSequencers(darknightSequencers, Darknight)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-166" in {
      val probe = TestProbe[CleanupResponse]()
      smRef ! Cleanup(RandomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(RandomObsMode))
    }
  }

  "StartSequencer" must {
    "transition sm from Idle -> Starting -> Idle state and start the sequencer for given obs mode | ESW-176" in {
      val componentId    = ComponentId(Prefix(ESW, Darknight.name), Sequencer)
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

    "return AlreadyRunning if sequencer for given obs mode is already running | ESW-176" in {
      val componentId    = ComponentId(Prefix(ESW, Darknight.name), Sequencer)
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

    "return Error if start sequencer returns error | ESW-176" in {
      val componentId           = ComponentId(Prefix(ESW, Darknight.name), Sequencer)
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

  "ShutdownSequencer" must {
    "transition sm from Idle -> ShuttingDown -> Idle state and shut down the sequencer for given obs mode | ESW-326" in {
      when(sequencerUtil.shutdownSequencer(ESW, Darknight))
        .thenReturn(future(1.seconds, Right(ShutdownSequencerResponse.Success)))

      val shutdownSequencerResponseProbe = TestProbe[ShutdownSequencerResponse]()

      assertState(Idle)
      smRef ! ShutdownSequencer(ESW, Darknight, shutdownSequenceComp = false, shutdownSequencerResponseProbe.ref)
      assertState(ShuttingDownSequencer)
      shutdownSequencerResponseProbe.expectMessage(ShutdownSequencerResponse.Success)
      assertState(Idle)

      verify(sequencerUtil).shutdownSequencer(ESW, Darknight)
    }

    "shutdown the sequence component along with sequencer | ESW-326, ESW-167" in {
      when(sequencerUtil.shutdownSequencer(ESW, Darknight, shutdownSequenceComp = true))
        .thenReturn(future(1.seconds, Right(ShutdownSequencerResponse.Success)))

      val shutdownSequencerResponseProbe = TestProbe[ShutdownSequencerResponse]()

      assertState(Idle)
      smRef ! ShutdownSequencer(ESW, Darknight, shutdownSequenceComp = true, shutdownSequencerResponseProbe.ref)
      assertState(ShuttingDownSequencer)
      shutdownSequencerResponseProbe.expectMessage(ShutdownSequencerResponse.Success)
      assertState(Idle)

      verify(sequencerUtil).shutdownSequencer(ESW, Darknight, shutdownSequenceComp = true)
    }

    "return UnloadScriptError if unload script fails | ESW-326" in {
      val prefix = Prefix(ESW, Darknight.name)
      when(sequencerUtil.shutdownSequencer(ESW, Darknight))
        .thenReturn(future(1.seconds, Left(UnloadScriptError(prefix, "something went wrong"))))

      val shutdownSequencerResponseProbe = TestProbe[ShutdownSequencerResponse]()

      smRef ! ShutdownSequencer(ESW, Darknight, shutdownSequenceComp = false, shutdownSequencerResponseProbe.ref)
      shutdownSequencerResponseProbe.expectMessage(UnloadScriptError(prefix, "something went wrong"))

      verify(sequencerUtil).shutdownSequencer(ESW, Darknight)
    }

    "return LocationServiceError if location service fails | ESW-326" in {
      when(sequencerUtil.shutdownSequencer(ESW, Darknight))
        .thenReturn(future(1.seconds, Left(LocationServiceError("something went wrong"))))

      val shutdownSequencerResponseProbe = TestProbe[ShutdownSequencerResponse]()

      smRef ! ShutdownSequencer(ESW, Darknight, shutdownSequenceComp = false, shutdownSequencerResponseProbe.ref)
      shutdownSequencerResponseProbe.expectMessage(LocationServiceError("something went wrong"))

      verify(sequencerUtil).shutdownSequencer(ESW, Darknight)
    }
  }

  "RestartSequencer" must {
    "transition sm from Idle -> Restarting -> Idle state and restart the sequencer for given obs mode | ESW-327" in {
      val prefix      = Prefix(ESW, Darknight.name)
      val componentId = ComponentId(prefix, Sequencer)

      when(sequencerUtil.restartSequencer(ESW, Darknight))
        .thenReturn(future(1.seconds, RestartSequencerResponse.Success(componentId)))

      val restartSequencerResponseProbe = TestProbe[RestartSequencerResponse]()

      assertState(Idle)
      smRef ! RestartSequencer(ESW, Darknight, restartSequencerResponseProbe.ref)
      assertState(RestartingSequencer)
      restartSequencerResponseProbe.expectMessage(RestartSequencerResponse.Success(componentId))
      assertState(Idle)

      verify(sequencerUtil).restartSequencer(ESW, Darknight)
    }

    val errors = Table(
      ("errorName", "error", "process"),
      ("UnloadScriptError", UnloadScriptError(Prefix(ESW, Darknight.name), "unload script error"), "stop"),
      ("LocationServiceError", LocationServiceError("location service error"), "stop"),
      ("SpawnSequenceComponentFailed", SpawnSequenceComponentFailed("spawn sequence component failed"), "start"),
      ("LoadScriptError", LoadScriptError("load script failed"), "start")
    )

    forAll(errors) { (errorName, error, process) =>
      s"return $errorName if $errorName encountered while sequencer $process | ESW-327" in {
        when(sequencerUtil.restartSequencer(ESW, Darknight))
          .thenReturn(future(1.seconds, error))

        val restartSequencerResponseProbe = TestProbe[RestartSequencerResponse]()

        smRef ! RestartSequencer(ESW, Darknight, restartSequencerResponseProbe.ref)
        restartSequencerResponseProbe.expectMessage(error)

        verify(sequencerUtil).restartSequencer(ESW, Darknight)
      }
    }
  }

  "ShutdownAllSequencers" must {
    "transition sm from Idle -> ShuttingDownAllSequencers -> Idle state and shut down all the currently running sequencers | ESW-324" in {
      when(sequencerUtil.shutdownAllSequencers())
        .thenReturn(future(1.seconds, ShutdownAllSequencersResponse.Success))

      val shutdownSequencerResponseProbe = TestProbe[ShutdownAllSequencersResponse]()

      assertState(Idle)
      smRef ! ShutdownAllSequencers(shutdownSequencerResponseProbe.ref)
      assertState(ShuttingDownAllSequencers)
      shutdownSequencerResponseProbe.expectMessage(ShutdownAllSequencersResponse.Success)
      assertState(Idle)

      verify(sequencerUtil).shutdownAllSequencers()
    }

    val errors = Table(
      ("errorName", "error", "process"),
      (
        "ShutDownFailure",
        ShutdownFailure(List(UnloadScriptError(Prefix(ESW, Darknight.name), "unload the script of any sequencer"))),
        "stop"
      ),
      ("LocationServiceError", LocationServiceError("location service error"), "listing all the running sequencers")
    )

    forAll(errors) { (errorName, error, process) =>
      s"return $errorName if $errorName encountered while $process | ESW-324" in {
        when(sequencerUtil.shutdownAllSequencers()).thenReturn(future(1.seconds, error))

        val shutdownSequencerResponseProbe = TestProbe[ShutdownAllSequencersResponse]()

        smRef ! ShutdownAllSequencers(shutdownSequencerResponseProbe.ref)
        shutdownSequencerResponseProbe.expectMessage(error)

        verify(sequencerUtil).shutdownAllSequencers()
      }
    }
  }

  "ShutdownSequenceComponent" must {
    "transition sm from Idle -> ShuttingDownSequenceComponent -> Idle state and shutdown the sequence component for given prefix | ESW-338" in {
      val prefix = Prefix(ESW, "primary")

      when(sequenceComponentUtil.shutdown(prefix)).thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      val shutdownSequenceComponentResponseProbe = TestProbe[ShutdownSequenceComponentResponse]()

      assertState(Idle)
      smRef ! ShutdownSequenceComponent(prefix, shutdownSequenceComponentResponseProbe.ref)
      assertState(ShuttingDownSequenceComponent)
      shutdownSequenceComponentResponseProbe.expectMessage(ShutdownSequenceComponentResponse.Success)
      assertState(Idle)

      verify(sequenceComponentUtil).shutdown(prefix)
    }

    val errors = Table(
      ("errorName", "error"),
      ("LocationServiceError", LocationServiceError("location service error")),
      ("SpawnSequenceComponentFailed", ShutdownSequenceComponentFailure("shutdown sequence component failed"))
    )

    forAll(errors) { (errorName, error) =>
      s"return $errorName if $errorName encountered while shutting down sequence component | ESW-338" in {
        val prefix = Prefix(ESW, "primary")

        when(sequenceComponentUtil.shutdown(prefix)).thenReturn(Future.successful(error))
        val shutdownSequenceComponentResponseProbe = TestProbe[ShutdownSequenceComponentResponse]()

        smRef ! ShutdownSequenceComponent(prefix, shutdownSequenceComponentResponseProbe.ref)
        shutdownSequenceComponentResponseProbe.expectMessage(error)

        verify(sequenceComponentUtil).shutdown(prefix)
      }
    }
  }

  "SpawnSequenceComponent" must {
    "transition sm from Idle -> SpawningSequenceComponent -> Idle state and spawn new sequence component | ESW-337" in {
      val seqCompName = "seq_comp"
      val machine     = ComponentId(Prefix(ESW, "primary"), Machine)
      val seqComp     = ComponentId(Prefix(ESW, seqCompName), SequenceComponent)
      when(sequenceComponentUtil.spawnSequenceComponent(machine, seqCompName))
        .thenReturn(future(1.seconds, SpawnSequenceComponentResponse.Success(seqComp)))

      val spawnSequenceComponentProbe = TestProbe[SpawnSequenceComponentResponse]()

      assertState(Idle)
      smRef ! SpawnSequenceComponent(machine, seqCompName, spawnSequenceComponentProbe.ref)
      assertState(SpawningSequenceComponent)
      spawnSequenceComponentProbe.expectMessage(SpawnSequenceComponentResponse.Success(seqComp))
      assertState(Idle)

      verify(sequenceComponentUtil).spawnSequenceComponent(machine, seqCompName)
    }

    "return LocationServiceError if location service gives error | ESW-337" in {
      val seqCompName = "seq_comp"
      val machine     = ComponentId(Prefix(ESW, "primary"), Machine)
      when(sequenceComponentUtil.spawnSequenceComponent(machine, seqCompName))
        .thenReturn(future(1.seconds, LocationServiceError("location service error")))

      val spawnSequenceComponentProbe = TestProbe[SpawnSequenceComponentResponse]()

      smRef ! SpawnSequenceComponent(machine, seqCompName, spawnSequenceComponentProbe.ref)
      spawnSequenceComponentProbe.expectMessage(LocationServiceError("location service error"))

      verify(sequenceComponentUtil).spawnSequenceComponent(machine, seqCompName)
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-337" in {
      val seqCompName = "seq_comp"
      val machine     = ComponentId(Prefix(ESW, "primary"), Machine)
      when(sequenceComponentUtil.spawnSequenceComponent(machine, seqCompName))
        .thenReturn(future(1.seconds, SpawnSequenceComponentFailed("spawning failed")))

      val spawnSequenceComponentProbe = TestProbe[SpawnSequenceComponentResponse]()

      smRef ! SpawnSequenceComponent(machine, seqCompName, spawnSequenceComponentProbe.ref)
      spawnSequenceComponentProbe.expectMessage(SpawnSequenceComponentFailed("spawning failed"))

      verify(sequenceComponentUtil).spawnSequenceComponent(machine, seqCompName)
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
