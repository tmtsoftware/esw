package esw.sm.utils

import java.net.URI

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.utils.location.LocationServiceUtil
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.sm.core.Sequencers
import esw.sm.messages.ConfigureResponse.{FailedToStartSequencers, Success}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtilTest extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")
  implicit val ec: ExecutionContext                       = system.executionContext
  implicit val timeout: Timeout                           = 5.seconds

  override def afterAll(): Unit = {
    super.afterAll()
    system.terminate()
  }

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

  "areSequencersIdle" must {
    "return true if all given sequencers are idle | ESW-178" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(Future.successful(eswLocation))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout)).thenReturn(Future.successful(tcsLocation))
      when(eswSequencerApi.isAvailable).thenReturn(Future.successful(true))
      when(tcsSequencerApi.isAvailable).thenReturn(Future.successful(true))

      val eventualBoolean = sequencerUtil.areSequencersIdle(Sequencers(ESW, TCS), obsMode)

      eventualBoolean.awaitResult shouldBe true
      verify(eswSequencerApi).isAvailable
      verify(tcsSequencerApi).isAvailable
    }

    "return false if one or more of given sequencers are busy | ESW-178" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(Future.successful(eswLocation))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout)).thenReturn(Future.successful(tcsLocation))
      when(eswSequencerApi.isAvailable).thenReturn(Future.successful(true))
      when(tcsSequencerApi.isAvailable).thenReturn(Future.successful(false))

      val eventualBoolean = sequencerUtil.areSequencersIdle(Sequencers(ESW, TCS), obsMode)

      eventualBoolean.awaitResult shouldBe false
      verify(eswSequencerApi).isAvailable
      verify(tcsSequencerApi).isAvailable
    }
  }

  "stopSequencers" must {
    "stop all the given sequencers" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(locationServiceUtil.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout)).thenReturn(Future.successful(eswLocation))
      when(locationServiceUtil.resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout)).thenReturn(Future.successful(tcsLocation))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Done))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.stopSequencers(Sequencers(ESW, TCS), obsMode).awaitResult shouldBe Done

      verify(eswSequencerApi).getSequenceComponent
      verify(tcsSequencerApi).getSequenceComponent

      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)

    }

  }

  class TestSetup(val obsMode: String) {

    val eswSeqComp: SequenceComponentImpl            = mock[SequenceComponentImpl]
    val tcsSeqComp: SequenceComponentImpl            = mock[SequenceComponentImpl]
    val locationService: LocationService             = mock[LocationService]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), Sequencer)), URI.create(""))
    val tcsLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), Sequencer)), URI.create(""))

    val sequencerUtil: SequencerUtil = new SequencerUtil(locationServiceUtil, sequenceComponentUtil) {
      override def createSequencerClient(location: Location): SequencerApi =
        if (location.prefix.subsystem == ESW) eswSequencerApi else tcsSequencerApi
    }

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
