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
import esw.sm.api.protocol.ShutdownSequencerResponse.UnloadScriptError
import esw.sm.api.protocol.StartSequencerResponse.LoadScriptError
import esw.sm.api.protocol.{CleanupResponse, RestartSequencerResponse, ShutdownAllSequencersResponse, ShutdownSequencerResponse}
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
      sequencerUtil.startSequencers(obsMode, Sequencers(ESW, TCS), 3).futureValue should ===(Success(eswComponentId))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
      verify(eswSeqComp).loadScript(ESW, obsMode)
      verify(tcsSeqComp).loadScript(TCS, obsMode)
    }

    "return all the errors caused while starting the sequencers  | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      // unable to start sequence component error
      val seqCompErrorMsg              = "could not spawn SeqComp for ESW"
      val spawnSequenceComponentFailed = futureLeft(SpawnSequenceComponentFailed(seqCompErrorMsg))
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(spawnSequenceComponentFailed)

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS ${obsMode.name}"
      val scriptError    = Future.successful(ScriptError.LoadingScriptFailed(scriptErrorMsg))
      when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(scriptError)

      sequencerUtil
        .startSequencers(obsMode, Sequencers(ESW, TCS), 3)
        .futureValue should ===(FailedToStartSequencers(Set(seqCompErrorMsg, scriptErrorMsg)))

      // getAvailableSequenceComponent for ESW returns SpawnSequenceComponentFailed so retry 3 times make total invocations 4
      // below verify validates 4 invocations
      verify(sequenceComponentUtil, times(4)).getAvailableSequenceComponent(ESW)

      // getAvailableSequenceComponent for ESW returns ScriptError so no retry
      // below verify validates 1 invocations
      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)

      verify(tcsSeqComp).loadScript(TCS, obsMode)
      verify(eswSeqComp, never).loadScript(ESW, obsMode)
    }
  }

  "startSequencer" must {
    "start given sequencer | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      sequencerUtil.startSequencer(ESW, obsMode, 3).rightValue should ===(eswLocation)

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(eswSeqComp).loadScript(ESW, obsMode)
    }

    "return error caused is spawn sequence component fails | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      val sequenceComponentFailedError = SpawnSequenceComponentFailed("could not spawn SeqComp for ESW")
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(futureLeft(sequenceComponentFailedError))

      sequencerUtil.startSequencer(ESW, obsMode, 3).leftValue should ===(sequenceComponentFailedError)

      verify(sequenceComponentUtil, times(4)).getAvailableSequenceComponent(ESW)
    }

    "retry if spawn sequence component fails | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      val sequenceComponentFailedError = SpawnSequenceComponentFailed("could not spawn SeqComp for ESW")
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW))
        .thenReturn(futureLeft(sequenceComponentFailedError), futureRight(eswSeqComp))

      sequencerUtil.startSequencer(ESW, obsMode, 3).rightValue should ===(eswLocation)

      verify(sequenceComponentUtil, times(2)).getAvailableSequenceComponent(ESW)
    }

    "retry if any other error than load script error | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      when(sequenceComponentUtil.getAvailableSequenceComponent(TCS))
        .thenReturn(futureRight(tcsSeqComp), futureRight(eswSeqComp))

      //mimic that meantime SM could start tcs sequencer on esw seqcomp, it is loaded another sequencer script
      when(tcsSeqComp.loadScript(TCS, obsMode))
        .thenReturn(Future.successful(Unhandled(Running, "already running")))

      when(eswSeqComp.loadScript(TCS, obsMode)).thenReturn(Future.successful(SequencerLocation(tcsLocation)))

      sequencerUtil.startSequencer(TCS, obsMode, 3).rightValue should ===(tcsLocation)

      verify(sequenceComponentUtil, times(2)).getAvailableSequenceComponent(TCS)
      verify(tcsSeqComp).loadScript(TCS, obsMode)
      verify(eswSeqComp).loadScript(TCS, obsMode)
    }

    "return error caused if loading script returns error and do not retry | ESW-176" in {
      val setup = new TestSetup()
      import setup._

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS ${obsMode.name}"
      val scriptError    = Future.successful(ScriptError.LoadingScriptFailed(scriptErrorMsg))
      when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(scriptError)

      sequencerUtil.startSequencer(TCS, obsMode, 3).leftValue should ===(LoadScriptError(scriptErrorMsg))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
    }
  }

  "shutdownSequencers" must {
    "shutdown all the given sequencers and return Done | ESW-166" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(locationServiceUtil.findSequencer(TCS, obsMode)).thenReturn(futureRight(tcsLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencers(Sequencers(ESW, TCS), obsMode).futureValue should ===(CleanupResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(tcsSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }

    "return Done even sequencer is not running | ESW-166" in {
      val setup = new TestSetup()
      import setup._

      // mimic the exception thrown from LocationServiceUtil.findSequencer
      val findFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(findFailed)

      sequencerUtil.shutdownSequencers(Sequencers(ESW), obsMode).futureValue should ===(CleanupResponse.Success)

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return RegistrationListingFailed when location service returns RegistrationListingFailed error | ESW-166" in {
      val setup = new TestSetup()
      import setup._
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))
      when(locationServiceUtil.findSequencer(TCS, obsMode)).thenReturn(futureRight(tcsLocation))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencers(Sequencers(ESW, TCS), obsMode).futureValue should ===(
        CleanupResponse.FailedToShutdownSequencers(Set("Error"))
      )

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(locationServiceUtil).findSequencer(TCS, obsMode)
      verify(tcsSequencerApi).getSequenceComponent
    }
  }

  "shutdownSequencer" must {
    "shutdown the given sequencer and return Done | ESW-326" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencer(ESW, obsMode).rightValue should ===(ShutdownSequencerResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
    }

    "shutdown the given sequence component along with sequencer and return Done | ESW-326, ESW-167" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(sequenceComponentUtil.shutdown(eswSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencer(ESW, obsMode, shutdownSequenceComp = true).rightValue should ===(
        ShutdownSequencerResponse.Success
      )

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).shutdown(eswSeqCompLoc)
    }

    "return Success even if sequencer is not running | ESW-326" in {
      val setup = new TestSetup()
      import setup._

      // mimic the exception thrown from LocationServiceUtil.findSequencer
      val findLocationFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(findLocationFailed)

      sequencerUtil.shutdownSequencer(ESW, obsMode).rightValue should ===(ShutdownSequencerResponse.Success)

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return Failure response when location service returns RegistrationListingFailed error | ESW-326" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.findSequencer(ESW, obsMode))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencer(ESW, obsMode).leftValue should ===(
        LocationServiceError("Error")
      )

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
    }

    "return Failure response when unload script future fails | ESW-326" in {
      val setup = new TestSetup()
      import setup._
      val prefix        = Prefix(ESW, obsMode.name)
      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(prefix, SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.failed(new TimeoutException("error")))

      sequencerUtil.shutdownSequencer(ESW, obsMode).leftValue should ===(UnloadScriptError(prefix, "error"))

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(locationServiceUtil).findSequencer(ESW, obsMode)
    }
  }

  "shutdownAllSequencers" must {
    "stop all the sequencers running | ESW-324" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureRight(List(eswLocation, tcsLocation)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownAllSequencers().futureValue should ===(ShutdownAllSequencersResponse.Success)

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-324" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownAllSequencers().futureValue should ===(LocationServiceError("Error"))
    }

    "return ShutdownFailure if any of the sequencer failed to shut down | ESW-324" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureRight(List(eswLocation, tcsLocation)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.failed(new RuntimeException("Error")))

      sequencerUtil.shutdownAllSequencers().futureValue should ===(
        ShutdownAllSequencersResponse.ShutdownFailure(List(UnloadScriptError(Prefix(TCS, obsMode.name), "Error")))
      )

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }
  }

  "restartSequencer" must {
    "restart given sequencer that is running | ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary.name"), SequenceComponent)), URI.create(""))
      val eswSeqLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), Sequencer)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.restart(eswSeqCompLoc)).thenReturn(Future.successful(SequencerLocation(eswSeqLoc)))

      sequencerUtil.restartSequencer(ESW, obsMode).futureValue should ===(RestartSequencerResponse.Success(eswComponentId))

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(sequenceComponentUtil).restart(eswSeqCompLoc)
    }

    "return LoadScriptError error if restart fails with LoadingScriptFailed | ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val errorMsg        = "loading script failed"
      val loadScriptError = LoadScriptError(errorMsg)
      val eswSeqCompLoc   = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.restart(eswSeqCompLoc))
        .thenReturn(Future.successful(ScriptError.LoadingScriptFailed(errorMsg)))

      sequencerUtil.restartSequencer(ESW, obsMode).futureValue should ===(loadScriptError)

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(sequenceComponentUtil).restart(eswSeqCompLoc)
    }

    "return LocationServiceError error if restart fails | ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val errorMsg      = "location not found"
      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureLeft(LocationNotFound("location not found")))

      sequencerUtil.restartSequencer(ESW, obsMode).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(sequenceComponentUtil, times(0)).restart(eswSeqCompLoc)
    }

    "return LoadScriptError error if restart fails with Unhandled| ESW-327" in {
      val setup = new TestSetup()
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode.name), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.findSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.restart(eswSeqCompLoc))
        .thenReturn(Future.successful(Unhandled(Idle, "Restart", "error")))

      sequencerUtil.restartSequencer(ESW, obsMode).futureValue should ===(LoadScriptError("error"))

      verify(locationServiceUtil).findSequencer(ESW, obsMode)
      verify(sequenceComponentUtil).restart(eswSeqCompLoc)
    }
  }

  class TestSetup() {
    val obsMode: ObsMode                             = ObsMode("darkNight")
    val eswSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val tcsSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswComponentId: ComponentId = ComponentId(Prefix(ESW, obsMode.name), Sequencer)

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(eswComponentId), URI.create(""))
    val tcsLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode.name), Sequencer)), URI.create(""))

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
    when(eswSeqComp.loadScript(ESW, obsMode)).thenReturn(Future.successful(SequencerLocation(eswLocation)))
    when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(Future.successful(SequencerLocation(tcsLocation)))

    val masterSeqConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, obsMode.name), Sequencer))
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""))
  }
}
