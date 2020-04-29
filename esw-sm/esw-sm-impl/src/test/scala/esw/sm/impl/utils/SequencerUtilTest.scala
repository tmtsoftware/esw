package esw.sm.impl.utils

import java.net.URI

import akka.Done
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, TCS}
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.commons.utils.location.{ComponentFactory, LocationServiceUtil}
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.ocs.api.protocol.{ScriptError, ScriptResponse}
import esw.sm.api.actor.messages.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.models.SequenceManagerError.{SequencerNotIdle, SpawnSequenceComponentFailed}
import esw.sm.api.models.Sequencers

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

      when(locationServiceUtil.resolve(masterSeqConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Right(masterSeqLocation)))

      sequencerUtil.resolveMasterSequencerOf(obsMode).rightValue shouldBe masterSeqLocation

      verify(locationServiceUtil).resolve(masterSeqConnection, Timeouts.DefaultTimeout)
    }

    "return the ResolveLocationFailed for the given obsMode  | ESW-178" in {
      val obsMode = "clearSky"
      val setup   = new TestSetup(obsMode)
      import setup._

      when(locationServiceUtil.resolve(masterSeqConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Left(ResolveLocationFailed("error"))))

      sequencerUtil.resolveMasterSequencerOf(obsMode).leftValue shouldBe ResolveLocationFailed("error")

      verify(locationServiceUtil).resolve(masterSeqConnection, Timeouts.DefaultTimeout)
    }

    "return the RegistrationListingFailed for the given obsMode  | ESW-178" in {
      val obsMode = "clearSky"
      val setup   = new TestSetup(obsMode)
      import setup._

      when(locationServiceUtil.resolve(masterSeqConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("error"))))

      sequencerUtil.resolveMasterSequencerOf(obsMode).leftValue shouldBe RegistrationListingFailed("error")

      verify(locationServiceUtil).resolve(masterSeqConnection, Timeouts.DefaultTimeout)
    }
  }

  "startSequencers" must {
    "start all the given sequencers | ESW-178" in {
      val obsMode = "darkNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      when(locationServiceUtil.resolve(masterSeqConnection, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Right(masterSeqLocation)))

      // returns success with master sequencer location after starting all the sequencers
      sequencerUtil.startSequencers(obsMode, Sequencers(ESW, TCS)).awaitResult shouldBe Success(masterSeqLocation)

      verify(locationServiceUtil).resolve(masterSeqConnection, Timeouts.DefaultTimeout)

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
      val seqCompErrorMsg = "could not spawn SeqComp for ESW"
      when(sequenceComponentUtil.getAvailableSequenceComponent(ESW))
        .thenReturn(Future.successful(Left(SpawnSequenceComponentFailed(seqCompErrorMsg))))

      // unable to loadScript script error
      val scriptErrorMsg = s"script initialisation failed for TCS $obsMode"
      when(tcsSeqComp.loadScript(TCS, obsMode))
        .thenReturn(Future.successful(ScriptResponse(Left(ScriptError(scriptErrorMsg)))))

      sequencerUtil
        .startSequencers(obsMode, Sequencers(ESW, TCS))
        .awaitResult
        .shouldBe(FailedToStartSequencers(Set(seqCompErrorMsg, scriptErrorMsg)))

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

  "areSequencersIdle" must {
    "return Done if all given sequencers are idle | ESW-178" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      when(componentFactory.resolveSequencer(ESW, obsMode)).thenReturn(Future.successful(Right(eswSequencerApi)))
      when(componentFactory.resolveSequencer(TCS, obsMode)).thenReturn(Future.successful(Right(tcsSequencerApi)))
      when(eswSequencerApi.isAvailable).thenReturn(Future.successful(true))
      when(tcsSequencerApi.isAvailable).thenReturn(Future.successful(true))

      val eventualBoolean = sequencerUtil.checkForSequencersAvailability(Sequencers(ESW, TCS), obsMode)

      eventualBoolean.rightValue shouldBe Done
      verify(eswSequencerApi).isAvailable
      verify(tcsSequencerApi).isAvailable
    }

    "return Error if one or more of given sequencers are busy | ESW-178" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      when(componentFactory.resolveSequencer(ESW, obsMode, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Right(eswSequencerApi)))
      when(componentFactory.resolveSequencer(TCS, obsMode, Timeouts.DefaultTimeout))
        .thenReturn(Future.successful(Right(tcsSequencerApi)))
      when(eswSequencerApi.isAvailable).thenReturn(Future.successful(true))
      when(tcsSequencerApi.isAvailable).thenReturn(Future.successful(false))

      val eventualBoolean = sequencerUtil.checkForSequencersAvailability(Sequencers(ESW, TCS), obsMode)

      eventualBoolean.leftValue shouldBe SequencerNotIdle(obsMode)
      verify(eswSequencerApi).isAvailable
      verify(tcsSequencerApi).isAvailable
    }
  }

  "stopSequencers" must {
    "stop all the given sequencers and return Done | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      val eswSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), SequenceComponent)), URI.create(""))
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(componentFactory.resolveSequencer(ESW, obsMode)).thenReturn(Future.successful(Right(eswSequencerApi)))
      when(componentFactory.resolveSequencer(TCS, obsMode)).thenReturn(Future.successful(Right(tcsSequencerApi)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswSeqCompLoc))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswSeqCompLoc)).thenReturn(Future.successful(Done))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.stopSequencers(Sequencers(ESW, TCS), obsMode).rightValue shouldBe Done

      verify(eswSequencerApi).getSequenceComponent
      verify(tcsSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswSeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsSeqCompLoc)
    }

    "return Done even sequencer is not running | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._

      when(componentFactory.resolveSequencer(ESW, obsMode))
        .thenReturn(
          Future.successful(Left(ResolveLocationFailed("location service error")))
        ) // mimic the exception thrown from LocationServiceUtil.resolveSequencer

      sequencerUtil.stopSequencers(Sequencers(ESW), obsMode).rightValue shouldBe Done

      verify(componentFactory).resolveSequencer(ESW, obsMode)
      verify(eswSequencerApi, times(0)).getSequenceComponent
    }

    "return RegistrationListingFailed when location service returns RegistrationListingFailed error | ESW-166" in {
      val obsMode = "moonNight"
      val setup   = new TestSetup(obsMode)
      import setup._
      val tcsSeqCompLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), SequenceComponent)), URI.create(""))

      when(componentFactory.resolveSequencer(ESW, obsMode))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("Error"))))
      when(componentFactory.resolveSequencer(TCS, obsMode))
        .thenReturn(Future.successful(Right(tcsSequencerApi)))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsSeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsSeqCompLoc)).thenReturn(Future.successful(Done))

      sequencerUtil.stopSequencers(Sequencers(ESW, TCS), obsMode).leftValue shouldBe RegistrationListingFailed("Error")

      verify(componentFactory).resolveSequencer(ESW, obsMode)
      verify(componentFactory).resolveSequencer(TCS, obsMode)
      verify(tcsSequencerApi).getSequenceComponent
    }

  }

  class TestSetup(val obsMode: String) {

    val eswSeqComp: SequenceComponentImpl            = mock[SequenceComponentImpl]
    val tcsSeqComp: SequenceComponentImpl            = mock[SequenceComponentImpl]
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val componentFactory: ComponentFactory           = mock[ComponentFactory]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, obsMode), Sequencer)), URI.create(""))
    val tcsLocation: AkkaLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(TCS, obsMode), Sequencer)), URI.create(""))

    val sequencerUtil: SequencerUtil = new SequencerUtil(componentFactory, locationServiceUtil, sequenceComponentUtil)

    when(sequenceComponentUtil.getAvailableSequenceComponent(ESW)).thenReturn(Future.successful(Right(eswSeqComp)))
    when(sequenceComponentUtil.getAvailableSequenceComponent(TCS)).thenReturn(Future.successful(Right(tcsSeqComp)))
    when(eswSeqComp.loadScript(ESW, obsMode)).thenReturn(Future.successful(ScriptResponse(Right(eswLocation))))
    when(tcsSeqComp.loadScript(TCS, obsMode)).thenReturn(Future.successful(ScriptResponse(Right(tcsLocation))))

    val masterSeqConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""))
  }
}
