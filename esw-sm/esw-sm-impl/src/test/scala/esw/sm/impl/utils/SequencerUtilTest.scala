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
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.models.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.models.SequenceManagerError.SpawnSequenceComponentFailed
import esw.sm.impl.config.Sequencers

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

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
      verifyMasterSequencerIsResolved()
    }
  }

  "startSequencers" must {
    "start all the given sequencers | ESW-178" in {
      val obsMode = "darkNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // returns success with master sequencer location after starting all the sequencers
      sequencerUtil.startSequencers(obsMode, Sequencers(ESW, TCS)).futureValue should ===(Success(masterSeqLocation))

      verifyMasterSequencerIsResolved()

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
      val scriptError    = Future.successful(ScriptResponse(Left(ScriptError(scriptErrorMsg))))
      when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(scriptError)

      sequencerUtil
        .startSequencers(obsMode, Sequencers(ESW, TCS))
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

  "stopSequencers" must {
    "stop all the given sequencers and return Done | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode)).thenReturn(futureRight(eswLocation))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode)).thenReturn(futureRight(tcsLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Done))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.stopSequencers(Sequencers(ESW, TCS), obsMode).rightValue should ===(Done)

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
      val resolveFailed = futureLeft(ResolveLocationFailed("location service error"))
      when(locationServiceUtil.resolveSequencer(ESW, obsMode)).thenReturn(resolveFailed)

      sequencerUtil.stopSequencers(Sequencers(ESW), obsMode).rightValue should ===(Done)

      verify(locationServiceUtil).resolveSequencer(ESW, obsMode)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return RegistrationListingFailed when location service returns RegistrationListingFailed error | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode)).thenReturn(futureLeft(RegistrationListingFailed("Error")))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode)).thenReturn(futureRight(tcsLocation))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.stopSequencers(Sequencers(ESW, TCS), obsMode).leftValue should ===(RegistrationListingFailed("Error"))

      verify(locationServiceUtil).resolveSequencer(ESW, obsMode)
      verify(locationServiceUtil).resolveSequencer(TCS, obsMode)
      verify(tcsSequencerApi).getSequenceComponent
    }
  }

  class TestSetup(val obsMode: String) {

    val eswSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val tcsSeqComp: SequenceComponentApi             = mock[SequenceComponentApi]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), Sequencer)), URI.create(""))
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

    def verifyMasterSequencerIsResolved(): Unit = {
      verify(locationServiceUtil).resolve(masterSeqConnection, Timeouts.DefaultTimeout)
    }
  }
}
