package esw.sm.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.utils.location.LocationServiceUtil
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.sm.core.Sequencers
import esw.sm.messages.ConfigureResponse.{FailedToStartSequencers, Success}

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtilTest extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")
  implicit val ec: ExecutionContext                       = system.executionContext
  implicit val timeout: Timeout                           = 5.seconds

  "resolveMasterSequencerFor" must {
    "return the master sequencer for the given obsMode  | ESW-178" in {
      val obsMode = "clearSky"
      val setup   = new TestSetup(obsMode)
      import setup._

      sequencerUtil.resolveMasterSequencerOf(obsMode).awaitResult shouldBe Some(masterSeqLocation)

      verifyMasterSequencerIsResolved()
    }
  }

  "startSequencers" must {
    "start all the given sequencers | ESW-178" in {
      val obsMode = "darkNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      // returns success with master sequencer location after starting all the sequencers
      sequencerUtil.startSequencers(obsMode, Sequencers(ESW, TCS)).awaitResult shouldBe Success(masterSeqLocation)

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

      // unable to find / start sequence component error
      val seqCompErrorMsg = "could not find SeqComp for ESW"
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW))
        .thenReturn(Future.successful(Left(SequencerError(seqCompErrorMsg))))

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS $obsMode"
      when(tcsSeqComp.loadScript(TCS, obsMode))
        .thenReturn(Future.successful(ScriptResponse(Left(ScriptError(scriptErrorMsg)))))

      sequencerUtil
        .startSequencers(obsMode, Sequencers(ESW, TCS))
        .awaitResult
        .shouldBe(FailedToStartSequencers(Set(seqCompErrorMsg, scriptErrorMsg)))

      verify(sequenceComponentUtil).getAvailableSequenceComponent(ESW)
      verify(sequenceComponentUtil).getAvailableSequenceComponent(TCS)
      verify(tcsSeqComp).loadScript(TCS, obsMode)
    }
  }

  class TestSetup(val obsMode: String) {
    val eswSeqComp: SequenceComponentImpl            = mock[SequenceComponentImpl]
    val tcsSeqComp: SequenceComponentImpl            = mock[SequenceComponentImpl]
    val locationService: LocationService             = mock[LocationService]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), Sequencer)), URI.create(""))
    val tcsLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), Sequencer)), URI.create(""))

    val sequencerUtil = new SequencerUtil(locationServiceUtil, sequenceComponentUtil)

    when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(Future.successful(Right(eswSeqComp)))
    when(sequenceComponentUtil.getAvailableSequenceComponent(TCS)).thenReturn(Future.successful(Right(tcsSeqComp)))
    when(eswSeqComp.loadScript(ESW, obsMode)).thenReturn(Future.successful(ScriptResponse(Right(eswLocation))))
    when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(Future.successful(ScriptResponse(Right(tcsLocation))))

    val masterSeqConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""))

    when(locationServiceUtil.locationService).thenReturn(locationService)
    when(locationService.resolve(masterSeqConnection, Timeouts.DefaultTimeout))
      .thenReturn(Future.successful(Some(masterSeqLocation)))

    def verifyMasterSequencerIsResolved(): Unit = {
      verify(locationService).resolve(masterSeqConnection, Timeouts.DefaultTimeout)
      verify(locationServiceUtil).locationService
    }
  }
}
