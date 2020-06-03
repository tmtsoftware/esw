package esw.sm.impl.utils

import java.net.URI

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.models.CommonFailure.LocationServiceError
import esw.sm.api.models.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.models.SequenceManagerError.{LoadScriptError, SpawnSequenceComponentFailed, UnloadScriptError}
import esw.sm.api.models.{CleanupResponse, ShutdownSequencerResponse}
import esw.sm.impl.config.Sequencers

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

  "resolveMasterSequencerFor" must {
    "return the master sequencer for the given obsMode  | ESW-178" in {
      val obsMode = "clearSky"
      val setup   = new TestSetup(obsMode)
      import setup._

      sequencerUtil.resolveMasterSequencerOf(obsMode).rightValue should ===(masterSeqLocation)

      verify(locationServiceUtil).resolve(masterSeqConnection, Timeouts.DefaultTimeout)
    }
  }

  "startSequencers" must {
    "start all the given sequencers | ESW-178" in {
      val obsMode = "darkNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // returns success with master sequencer location after starting all the sequencers
      sequencerUtil.startSequencers(obsMode, Sequencers(ESW, TCS), 3).futureValue should ===(Success(eswComponentId))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
      verify(eswSeqComp).loadScript(ESW, obsMode)
      verify(tcsSeqComp).loadScript(TCS, obsMode)
    }

    "return all the errors caused while starting the sequencers  | ESW-178" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // unable to start sequence component error
      val seqCompErrorMsg              = "could not spawn SeqComp for ESW"
      val spawnSequenceComponentFailed = futureLeft(SpawnSequenceComponentFailed(seqCompErrorMsg))
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(spawnSequenceComponentFailed)

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS $obsMode"
      val scriptError    = Future.successful(ScriptResponse(Left(ScriptError.LoadingScriptFailed(scriptErrorMsg))))
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
      val obsMode = "darknight"
      val setup   = new TestSetup(obsMode)
      import setup._

      sequencerUtil.startSequencer(ESW, obsMode, 3).rightValue should ===(eswLocation)

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(eswSeqComp).loadScript(ESW, obsMode)
    }

    "return error caused is spawn sequence component fails | ESW-176" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val sequenceComponentFailedError = SpawnSequenceComponentFailed("could not spawn SeqComp for ESW")
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(futureLeft(sequenceComponentFailedError))

      sequencerUtil.startSequencer(ESW, obsMode, 3).leftValue should ===(sequenceComponentFailedError)

      verify(sequenceComponentUtil, times(4)).getAvailableSequenceComponent(ESW)
    }

    "retry is spawn sequence component fails | ESW-176" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val sequenceComponentFailedError = SpawnSequenceComponentFailed("could not spawn SeqComp for ESW")
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW))
        .thenReturn(futureLeft(sequenceComponentFailedError), futureRight(eswSeqComp))

      sequencerUtil.startSequencer(ESW, obsMode, 3).rightValue should ===(eswLocation)

      verify(sequenceComponentUtil, times(2)).getAvailableSequenceComponent(ESW)
    }

    "return error caused if loading script returns error | ESW-176" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS $obsMode"
      val scriptError    = Future.successful(ScriptResponse(Left(ScriptError.LoadingScriptFailed(scriptErrorMsg))))
      when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(scriptError)

      sequencerUtil.startSequencer(TCS, obsMode, 3).leftValue should ===(LoadScriptError(scriptErrorMsg))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
    }
  }

  "shutdownSequencers" must {
    "shutdown all the given sequencers and return Done | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(futureRight(eswLocation))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout)).thenReturn(futureRight(tcsLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Done))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.shutdownSequencers(Sequencers(ESW, TCS), obsMode).futureValue should ===(CleanupResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(tcsSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }

    "return Done even sequencer is not running | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // mimic the exception thrown from LocationServiceUtil.resolveSequencer
      val resolveFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(resolveFailed)

      sequencerUtil.shutdownSequencers(Sequencers(ESW), obsMode).futureValue should ===(CleanupResponse.Success)

      verify(locationServiceUtil).resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return RegistrationListingFailed when location service returns RegistrationListingFailed error | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout)).thenReturn(futureRight(tcsLocation))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.shutdownSequencers(Sequencers(ESW, TCS), obsMode).futureValue should ===(
        CleanupResponse.FailedToShutdownSequencers(Set("Error"))
      )

      verify(locationServiceUtil).resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)
      verify(locationServiceUtil).resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout)
      verify(tcsSequencerApi).getSequenceComponent
    }
  }

  "shutdownSequencer" must {
    "shutdown the given sequencers and return Done | ESW-326" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.shutdownSequencer(ESW, obsMode).rightValue should ===(ShutdownSequencerResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
    }

    "return Success even sequencer is not running | ESW-326" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // mimic the exception thrown from LocationServiceUtil.resolveSequencer
      val resolveFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(resolveFailed)

      sequencerUtil.shutdownSequencer(ESW, obsMode).rightValue should ===(ShutdownSequencerResponse.Success)

      verify(locationServiceUtil).resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return Failure response when location service returns RegistrationListingFailed error | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencer(ESW, obsMode).leftValue should ===(
        LocationServiceError("Error")
      )

      verify(locationServiceUtil).resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)
    }

    "return Failure response when unload script future fails | ESW-326" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(futureRight(eswLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.failed(new TimeoutException("error")))

      sequencerUtil.shutdownSequencer(ESW, obsMode).leftValue should ===(UnloadScriptError("error"))

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(locationServiceUtil).resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)
    }
  }

  class TestSetup(val obsMode: String) {

    val eswSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val tcsSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswComponentId: ComponentId = ComponentId(Prefix(ESW, obsMode), Sequencer)

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(eswComponentId), URI.create(""))
    val tcsLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), Sequencer)), URI.create(""))

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
    when(eswSeqComp.loadScript(ESW, obsMode)).thenReturn(Future.successful(ScriptResponse(Right(eswLocation))))
    when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(Future.successful(ScriptResponse(Right(tcsLocation))))

    val masterSeqConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""))

    when(locationServiceUtil.resolve(masterSeqConnection, Timeouts.DefaultTimeout)).thenReturn(futureRight(masterSeqLocation))
  }
}
