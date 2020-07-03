package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.ocs.api.models.SequenceComponentState.{Idle, Running}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, SequencerLocation, Unhandled}
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.protocol.AgentError.SpawnSequenceComponentFailed
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.protocol.RestartSequencerResponse.UnloadScriptError
import esw.sm.api.protocol.ShutdownSequencersResponse.ShutdownFailure
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.api.protocol.{RestartSequencerResponse, ShutdownSequencersPolicy, ShutdownSequencersResponse}
import esw.sm.impl.config.Sequencers
import esw.testcommons.BaseTestSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future, TimeoutException}

class SequencerUtilTest extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")
  implicit val ec: ExecutionContext                       = system.executionContext
  implicit val timeout: Timeout                           = 5.seconds

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  "startSequencers" must {
    "start all the given sequencers | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      // returns success with master sequencer location after starting all the sequencers
      sequencerUtil.startSequencers(darkNightObsMode, Sequencers(ESW, TCS), 3).futureValue should ===(Success(eswComponentId))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
      verify(eswSeqComp).loadScript(ESW, darkNightObsMode)
      verify(tcsSeqComp).loadScript(TCS, darkNightObsMode)
    }

    "return all the errors caused while starting the sequencers  | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      // unable to start sequence component error
      val seqCompErrorMsg              = "could not spawn SeqComp for ESW"
      val spawnSequenceComponentFailed = futureLeft(SpawnSequenceComponentFailed(seqCompErrorMsg))
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(spawnSequenceComponentFailed)

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS ${darkNightObsMode.name}"
      val scriptError    = Future.successful(ScriptError.LoadingScriptFailed(scriptErrorMsg))
      when(tcsSeqComp.loadScript(TCS, darkNightObsMode)).thenReturn(scriptError)

      sequencerUtil
        .startSequencers(darkNightObsMode, Sequencers(ESW, TCS), 3)
        .futureValue should ===(FailedToStartSequencers(Set(seqCompErrorMsg, scriptErrorMsg)))

      // getAvailableSequenceComponent for ESW returns SpawnSequenceComponentFailed so retry 3 times make total invocations 4
      // below verify validates 4 invocations
      verify(sequenceComponentUtil, times(4)).getAvailableSequenceComponent(ESW)

      // getAvailableSequenceComponent for ESW returns ScriptError so no retry
      // below verify validates 1 invocations
      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)

      verify(tcsSeqComp).loadScript(TCS, darkNightObsMode)
      verify(eswSeqComp, never).loadScript(ESW, darkNightObsMode)
    }
  }

  "startSequencer" must {
    "start given sequencer | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      sequencerUtil.startSequencer(ESW, darkNightObsMode, 3).rightValue should ===(eswLocation)

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(eswSeqComp).loadScript(ESW, darkNightObsMode)
    }

    "return error caused is spawn sequence component fails | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      val sequenceComponentFailedError = SpawnSequenceComponentFailed("could not spawn SeqComp for ESW")
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(futureLeft(sequenceComponentFailedError))

      sequencerUtil.startSequencer(ESW, darkNightObsMode, 3).leftValue should ===(sequenceComponentFailedError)

      verify(sequenceComponentUtil, times(4)).getAvailableSequenceComponent(ESW)
    }

    "retry if spawn sequence component fails | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      val sequenceComponentFailedError = SpawnSequenceComponentFailed("could not spawn SeqComp for ESW")
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW))
        .thenReturn(futureLeft(sequenceComponentFailedError), futureRight(eswSeqComp))

      sequencerUtil.startSequencer(ESW, darkNightObsMode, 3).rightValue should ===(eswLocation)

      verify(sequenceComponentUtil, times(2)).getAvailableSequenceComponent(ESW)
    }

    "retry if any other error than load script error | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      when(sequenceComponentUtil.getAvailableSequenceComponent(TCS))
        .thenReturn(futureRight(tcsSeqComp), futureRight(eswSeqComp))

      //mimic that meantime SM could start tcs sequencer on esw seqcomp, it is loaded another sequencer script
      when(tcsSeqComp.loadScript(TCS, darkNightObsMode))
        .thenReturn(Future.successful(Unhandled(Running, "already running")))

      when(eswSeqComp.loadScript(TCS, darkNightObsMode)).thenReturn(Future.successful(SequencerLocation(tcsLocation)))

      sequencerUtil.startSequencer(TCS, darkNightObsMode, 3).rightValue should ===(tcsLocation)

      verify(sequenceComponentUtil, times(2)).getAvailableSequenceComponent(TCS)
      verify(tcsSeqComp).loadScript(TCS, darkNightObsMode)
      verify(eswSeqComp).loadScript(TCS, darkNightObsMode)
    }

    "return error caused if loading script returns error and do not retry | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS ${darkNightObsMode.name}"
      val scriptError    = Future.successful(ScriptError.LoadingScriptFailed(scriptErrorMsg))
      when(tcsSeqComp.loadScript(TCS, darkNightObsMode)).thenReturn(scriptError)

      sequencerUtil.startSequencer(TCS, darkNightObsMode, 3).leftValue should ===(LoadScriptError(scriptErrorMsg))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
    }
  }

  "ShutdownSequencersPolicy.SingleSequencer" must {
    "shutdown the given sequencer and return Done | ESW-326" in {
      val setup = new TestSetup()
      import setup._

      val policy = ShutdownSequencersPolicy.SingleSequencer(ESW, darkNightObsMode)
      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencers(policy).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
    }

    "return Success even if sequencer is not running | ESW-326" in {
      val setup = new TestSetup()
      import setup._
      val policy = ShutdownSequencersPolicy.SingleSequencer(ESW, darkNightObsMode)
      // mimic the exception thrown from LocationServiceUtil.findSequencer
      val findLocationFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode)).thenReturn(findLocationFailed)

      sequencerUtil.shutdownSequencers(policy).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return Failure response when location service returns RegistrationListingFailed error | ESW-326" in {
      val setup = new TestSetup()
      import setup._
      val policy = ShutdownSequencersPolicy.SingleSequencer(ESW, darkNightObsMode)
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencers(policy).futureValue should ===(LocationServiceError("Error"))

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
    }

    "return Failure response when unload script future fails | ESW-326" in {
      val setup = new TestSetup()
      import setup._
      val prefix        = Prefix(ESW, darkNightObsMode.name)
      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(prefix, SequenceComponent)), URI.create(""))

      val policy = ShutdownSequencersPolicy.SingleSequencer(ESW, darkNightObsMode)
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.failed(new TimeoutException("error")))

      sequencerUtil.shutdownSequencers(policy).futureValue should ===(ShutdownFailure(List(UnloadScriptError(prefix, "error"))))

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
    }
  }

  "ShutdownSequencersPolicy.AllSequencers" must {
    val allShutdownPolicy = ShutdownSequencersPolicy.AllSequencers

    "stop all the sequencers running | ESW-324" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureRight(List(eswLocation, tcsLocation)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencers(allShutdownPolicy).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-324" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencers(allShutdownPolicy).futureValue should ===(LocationServiceError("Error"))
    }

    "return ShutdownFailure if any of the sequencer failed to shut down | ESW-324" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureRight(List(eswLocation, tcsLocation)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.failed(new RuntimeException("Error")))

      sequencerUtil.shutdownSequencers(allShutdownPolicy).futureValue should ===(
        ShutdownSequencersResponse.ShutdownFailure(List(UnloadScriptError(Prefix(TCS, darkNightObsMode.name), "Error")))
      )

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }
  }

  "ShutdownSequencersPolicy.ObsModeSequencers" must {
    val obsModeShutdownPolicy = ShutdownSequencersPolicy.ObsModeSequencers

    "stop all the sequencers running for specified Obs Mode | ESW-166" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.listAkkaLocationsBy(darkNightObsMode.name, Sequencer))
        .thenReturn(futureRight(List(eswLocation, tcsLocation)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencers(obsModeShutdownPolicy(darkNightObsMode)).futureValue should ===(
        ShutdownSequencersResponse.Success
      )

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-166" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(darkNightObsMode.name, Sequencer))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencers(obsModeShutdownPolicy(darkNightObsMode)).futureValue should ===(
        LocationServiceError("Error")
      )
    }

    "return ShutdownFailure if any of the sequencer failed to shut down | ESW-166" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.listAkkaLocationsBy(darkNightObsMode.name, Sequencer))
        .thenReturn(futureRight(List(eswLocation, tcsLocation)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.failed(new RuntimeException("Error")))

      sequencerUtil.shutdownSequencers(obsModeShutdownPolicy(darkNightObsMode)).futureValue should ===(
        ShutdownSequencersResponse.ShutdownFailure(List(UnloadScriptError(Prefix(TCS, darkNightObsMode.name), "Error")))
      )

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }
  }

  "ShutdownSequencersPolicy.SubsystemSequencers" must {
    val obsModeShutdownPolicy = ShutdownSequencersPolicy.SubsystemSequencers

    "stop all the sequencers running for specified subsystem | ESW-345" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(futureRight(List(eswDarkNightSequencer, eswClearSkiesSequencer)))
      when(eswSequencerApi.getSequenceComponent)
        .thenReturn(Future.successful(seqCompRunningDarkNight), Future.successful(seqCompRunningClearSkies))
      when(sequenceComponentUtil.unloadScript(seqCompRunningDarkNight)).thenReturn(Future.successful(Ok))
      when(sequenceComponentUtil.unloadScript(seqCompRunningClearSkies)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencers(obsModeShutdownPolicy(ESW)).futureValue should ===(
        ShutdownSequencersResponse.Success
      )

      verify(sequenceComponentUtil).unloadScript(seqCompRunningDarkNight)
      verify(sequenceComponentUtil).unloadScript(seqCompRunningClearSkies)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-345" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencers(obsModeShutdownPolicy(ESW)).futureValue should ===(
        LocationServiceError("Error")
      )
    }

    "return ShutdownFailure if any of the sequencer failed to shut down | ESW-345" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(futureRight(List(eswDarkNightSequencer, eswClearSkiesSequencer)))
      when(eswSequencerApi.getSequenceComponent)
        .thenReturn(Future.successful(seqCompRunningDarkNight), Future.successful(seqCompRunningClearSkies))
      when(sequenceComponentUtil.unloadScript(seqCompRunningDarkNight)).thenReturn(Future.successful(Ok))
      when(sequenceComponentUtil.unloadScript(seqCompRunningClearSkies)).thenReturn(Future.failed(new RuntimeException("Error")))

      sequencerUtil.shutdownSequencers(obsModeShutdownPolicy(ESW)).futureValue should ===(
        ShutdownSequencersResponse.ShutdownFailure(List(UnloadScriptError(Prefix(ESW, clearSkiesObsMode.name), "Error")))
      )

      verify(sequenceComponentUtil).unloadScript(seqCompRunningDarkNight)
      verify(sequenceComponentUtil).unloadScript(seqCompRunningClearSkies)
    }
  }

  "restartSequencer" must {
    "restart given sequencer that is running | ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary.name"), SequenceComponent)), URI.create(""))
      val eswSeqLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), Sequencer)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.restart(eswSeqCompLoc)).thenReturn(Future.successful(SequencerLocation(eswSeqLoc)))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(
        RestartSequencerResponse.Success(eswComponentId)
      )

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
      verify(sequenceComponentUtil).restart(eswSeqCompLoc)
    }

    "return LoadScriptError error if restart fails with LoadingScriptFailed | ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val errorMsg        = "loading script failed"
      val loadScriptError = LoadScriptError(errorMsg)
      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.restart(eswSeqCompLoc))
        .thenReturn(Future.successful(ScriptError.LoadingScriptFailed(errorMsg)))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(loadScriptError)

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
      verify(sequenceComponentUtil).restart(eswSeqCompLoc)
    }

    "return LocationServiceError error if restart fails | ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val errorMsg = "location not found"
      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode))
        .thenReturn(futureLeft(LocationNotFound("location not found")))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
      verify(sequenceComponentUtil, times(0)).restart(eswSeqCompLoc)
    }

    "return LoadScriptError error if restart fails with Unhandled| ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.restart(eswSeqCompLoc))
        .thenReturn(Future.successful(Unhandled(Idle, "Restart", "error")))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(LoadScriptError("error"))

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode)
      verify(sequenceComponentUtil).restart(eswSeqCompLoc)
    }
  }

  class TestSetup() {
    val darkNightObsMode: ObsMode                    = ObsMode("darkNight")
    val clearSkiesObsMode: ObsMode                   = ObsMode("clearSkies")
    val eswSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val tcsSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswComponentId: ComponentId = ComponentId(Prefix(ESW, darkNightObsMode.name), Sequencer)

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(eswComponentId), URI.create(""))
    val tcsLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, darkNightObsMode.name), Sequencer)), URI.create(""))
    val eswDarkNightSequencer: AkkaLocation    = akkaLocation(eswComponentId)
    val eswClearSkiesSequencer: AkkaLocation   = akkaLocation(ComponentId(Prefix(ESW, clearSkiesObsMode.name), Sequencer))
    val seqCompRunningDarkNight: AkkaLocation  = akkaLocation(ComponentId(Prefix(ESW, "primary"), SequenceComponent))
    val seqCompRunningClearSkies: AkkaLocation = akkaLocation(ComponentId(Prefix(ESW, "secondary"), SequenceComponent))

    val sequencerUtil: SequencerUtil = new SequencerUtil(locationServiceUtil, sequenceComponentUtil) {
      override private[sm] def createSequencerClient(location: Location) =
        location.prefix.subsystem match {
          case ESW => eswSequencerApi
          case TCS => tcsSequencerApi
          case _   => mock[SequencerApi]
        }
    }

    when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(Future.successful(Right(eswSeqComp)))
    when(sequenceComponentUtil.getAvailableSequenceComponent(TCS)).thenReturn(Future.successful(Right(tcsSeqComp)))
    when(eswSeqComp.loadScript(ESW, darkNightObsMode)).thenReturn(Future.successful(SequencerLocation(eswLocation)))
    when(tcsSeqComp.loadScript(TCS, darkNightObsMode)).thenReturn(Future.successful(SequencerLocation(tcsLocation)))

    val masterSeqConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, darkNightObsMode.name), Sequencer))
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""))

    def akkaLocation(componentId: ComponentId): AkkaLocation = AkkaLocation(AkkaConnection(componentId), URI.create(""))
  }
}
