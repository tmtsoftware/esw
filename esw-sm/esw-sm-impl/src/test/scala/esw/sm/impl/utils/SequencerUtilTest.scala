package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.ObsMode
import esw.ocs.api.models.SequenceComponentState.Idle
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, SequencerLocation, Unhandled}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.api.protocol.{RestartSequencerResponse, ShutdownSequencersResponse}
import esw.sm.impl.config.Sequencers
import esw.testcommons.BaseTestSuite

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class SequencerUtilTest extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")
  implicit val ec: ExecutionContext                       = system.executionContext
  implicit val timeout: Timeout                           = 5.seconds

  private val setup = new TestSetup()
  import setup._

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(locationServiceUtil, sequenceComponentUtil, eswSequencerApi, tcsSequencerApi)
  }

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  "startSequencersByMapping" must {
    "start all the given sequencers | ESW-178" in {
      when(sequenceComponentUtil.loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(eswDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))

      // returns success with master sequencer location after starting all the sequencers
      val response = sequencerUtil
        .startSequencersByMapping(
          darkNightObsMode,
          List((ESW, eswPrimarySeqCompLoc), (TCS, tcsPrimarySeqCompLoc))
        )
        .futureValue

      response should ===(Success(eswDarkNightSequencer))
      verify(sequenceComponentUtil).loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc)
    }

    "start all the given sequencers concurrently | ESW-178" in {
      when(sequenceComponentUtil.loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc))
        .thenReturn(future(1.seconds, Right(Started(eswDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc))
        .thenReturn(future(1.seconds, Right(Started(tcsDarkNightSequencer))))

      // each load script call taking 1 second
      // waiting for 1 second (200 millis processing time) will ensure that loading script is happening concurrently
      val response = Await.result(
        sequencerUtil
          .startSequencersByMapping(
            darkNightObsMode,
            List((ESW, eswPrimarySeqCompLoc), (TCS, tcsPrimarySeqCompLoc))
          ),
        1200.millis
      )

      response should ===(Success(eswDarkNightSequencer))
      verify(sequenceComponentUtil).loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc)
    }

    "return all the errors caused while starting the sequencers  | ESW-178" in {
      // load script error for ESW
      val loadScriptErrorMsg = "load script error"
      val loadScriptError    = futureLeft(LoadScriptError(loadScriptErrorMsg))
      when(sequenceComponentUtil.loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc)).thenReturn(loadScriptError)
      when(sequenceComponentUtil.loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))

      val response = sequencerUtil
        .startSequencersByMapping(
          darkNightObsMode,
          List((ESW, eswPrimarySeqCompLoc), (TCS, tcsPrimarySeqCompLoc))
        )
        .futureValue

      response should ===(FailedToStartSequencers(Set(loadScriptErrorMsg)))
      verify(sequenceComponentUtil).loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc)
    }
  }

  "restartSequencer" must {
    "restart given sequencer that is running | ESW-327" in {
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.restartScript(eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(SequencerLocation(eswDarkNightSequencerLoc)))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(
        RestartSequencerResponse.Success(eswDarkNightSequencer)
      )

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode.name)
      verify(sequenceComponentUtil).restartScript(eswPrimarySeqCompLoc)
    }

    "return LoadScriptError error if restart fails with LoadingScriptFailed | ESW-327" in {
      val errorMsg        = "loading script failed"
      val loadScriptError = LoadScriptError(errorMsg)
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.restartScript(eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(ScriptError.LoadingScriptFailed(errorMsg)))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(loadScriptError)

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode.name)
      verify(sequenceComponentUtil).restartScript(eswPrimarySeqCompLoc)
    }

    "return LocationServiceError error if restart fails | ESW-327" in {
      val errorMsg = "location not found"
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name))
        .thenReturn(futureLeft(LocationNotFound("location not found")))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode.name)
      verify(sequenceComponentUtil, never).restartScript(eswPrimarySeqCompLoc)
    }

    "return LoadScriptError error if restart fails with Unhandled| ESW-327" in {
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.restartScript(eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Unhandled(Idle, "Restart", "error")))

      sequencerUtil.restartSequencer(ESW, darkNightObsMode).futureValue should ===(LoadScriptError("error"))

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode.name)
      verify(sequenceComponentUtil).restartScript(eswPrimarySeqCompLoc)
    }
  }

  "shutdownSequencer" must {
    "shutdown the given sequencer and return Done | ESW-326, ESW-351" in {
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencer(ESW, darkNightObsMode).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswPrimarySeqCompLoc)
    }

    "return Success even if sequencer is not running | ESW-326, ESW-351" in {
      // mimic the exception thrown from LocationServiceUtil.findSequencer
      val findLocationFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name)).thenReturn(findLocationFailed)

      sequencerUtil.shutdownSequencer(ESW, darkNightObsMode).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode.name)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return Failure response when location service returns RegistrationListingFailed error | ESW-326, ESW-351" in {
      when(locationServiceUtil.findSequencer(ESW, darkNightObsMode.name))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencer(ESW, darkNightObsMode).futureValue should ===(LocationServiceError("Error"))

      verify(locationServiceUtil).findSequencer(ESW, darkNightObsMode.name)
    }
  }

  "shutdownSubsystemSequencers" must {
    "stop all the sequencers running for specified subsystem | ESW-345, ESW-351" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(futureRight(List(eswDarkNightSequencerLoc, eswClearSkiesSequencerLoc)))
      when(eswSequencerApi.getSequenceComponent)
        .thenReturn(Future.successful(eswPrimarySeqCompLoc), Future.successful(eswSecondarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))
      when(sequenceComponentUtil.unloadScript(eswSecondarySeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSubsystemSequencers(ESW).futureValue should ===(
        ShutdownSequencersResponse.Success
      )

      verify(sequenceComponentUtil).unloadScript(eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(eswSecondarySeqCompLoc)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-345, ESW-351" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSubsystemSequencers(ESW).futureValue should ===(
        LocationServiceError("Error")
      )
    }
  }

  "shutdownObsModeSequencers" must {
    "stop all the sequencers running for specified Obs Mode | ESW-166, ESW-351" in {
      when(locationServiceUtil.listAkkaLocationsBy(darkNightObsMode.name, Sequencer))
        .thenReturn(futureRight(List(eswDarkNightSequencerLoc, tcsDarkNightSequencerLoc)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsPrimarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownObsModeSequencers(darkNightObsMode).futureValue should ===(
        ShutdownSequencersResponse.Success
      )

      verify(sequenceComponentUtil).unloadScript(eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsPrimarySeqCompLoc)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-166, ESW-351" in {
      when(locationServiceUtil.listAkkaLocationsBy(darkNightObsMode.name, Sequencer))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownObsModeSequencers(darkNightObsMode).futureValue should ===(
        LocationServiceError("Error")
      )
    }

  }

  "shutdownAllSequencers" must {
    "stop all the sequencers running | ESW-324, ESW-351" in {
      when(locationServiceUtil.listAkkaLocationsBy(Sequencer))
        .thenReturn(futureRight(List(eswDarkNightSequencerLoc, tcsDarkNightSequencerLoc)))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))
      when(tcsSequencerApi.getSequenceComponent).thenReturn(Future.successful(tcsPrimarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(tcsPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownAllSequencers().futureValue should ===(ShutdownSequencersResponse.Success)

      verify(sequenceComponentUtil).unloadScript(eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).unloadScript(tcsPrimarySeqCompLoc)
    }

    "return LocationServiceError response when location service returns RegistrationListingFailed error | ESW-324, ESW-351" in {
      when(locationServiceUtil.listAkkaLocationsBy(Sequencer)).thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownAllSequencers().futureValue should ===(LocationServiceError("Error"))
    }
  }

  "startSequencers" must {
    "return success when adequate idle sequence components are available and all sequencers are started successfully | ESW-178" in {
      val sequencers = Sequencers(IRIS, ESW, TCS)

      when(sequenceComponentUtil.allocateSequenceComponents(sequencers))
        .thenReturn(futureRight(List((IRIS, eswPrimarySeqCompLoc), (ESW, eswSecondarySeqCompLoc), (TCS, tcsPrimarySeqCompLoc))))
      when(sequenceComponentUtil.loadScript(ESW, darkNightObsMode, eswSecondarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(eswDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(IRIS, darkNightObsMode, eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(irisDarkNightSequencer))))

      sequencerUtil.startSequencers(darkNightObsMode, sequencers).futureValue should ===(Success(eswDarkNightSequencer))

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencers)
      verify(sequenceComponentUtil).loadScript(ESW, darkNightObsMode, eswSecondarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(IRIS, darkNightObsMode, eswPrimarySeqCompLoc)
    }

    "return failure when adequate sequence components are not available to start sequencers | ESW-178, ESW-340" in {
      val sequencers = Sequencers(IRIS, ESW, TCS)

      when(sequenceComponentUtil.allocateSequenceComponents(sequencers))
        .thenReturn(futureLeft(SequenceComponentNotAvailable(List(ESW))))

      sequencerUtil
        .startSequencers(darkNightObsMode, sequencers)
        .futureValue should ===(SequenceComponentNotAvailable(List(ESW)))

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencers)
    }

    "return failure when location service error | ESW-178" in {
      val sequencers = Sequencers(IRIS, ESW, TCS)

      when(sequenceComponentUtil.allocateSequenceComponents(sequencers))
        .thenReturn(Future.successful(Left(LocationServiceError("error"))))

      sequencerUtil.startSequencers(darkNightObsMode, sequencers).futureValue should ===(
        LocationServiceError("error")
      )

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencers)
    }

    "return failure when load script fails | ESW-178" in {
      val sequencers = Sequencers(IRIS, ESW, TCS)
      val eswError   = "error for esw sequencer"
      val irisError  = "error for iris sequencer"

      when(sequenceComponentUtil.allocateSequenceComponents(sequencers))
        .thenReturn(futureRight(List((IRIS, irisPrimarySeqCompLoc), (ESW, eswPrimarySeqCompLoc), (TCS, tcsPrimarySeqCompLoc))))
      when(sequenceComponentUtil.loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Left(LoadScriptError(eswError))))
      when(sequenceComponentUtil.loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(IRIS, darkNightObsMode, irisPrimarySeqCompLoc))
        .thenReturn(Future.successful(Left(LoadScriptError(irisError))))

      sequencerUtil.startSequencers(darkNightObsMode, Sequencers(IRIS, ESW, TCS)).futureValue should ===(
        FailedToStartSequencers(Set(eswError, irisError))
      )

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencers)
      verify(sequenceComponentUtil).loadScript(ESW, darkNightObsMode, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(TCS, darkNightObsMode, tcsPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(IRIS, darkNightObsMode, irisPrimarySeqCompLoc)
    }
  }

  class TestSetup() {
    val darkNightObsMode: ObsMode  = ObsMode("darkNight")
    val clearSkiesObsMode: ObsMode = ObsMode("clearSkies")

    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
    val eswSequencerApi: SequencerApi                = mock[SequencerApi]
    val tcsSequencerApi: SequencerApi                = mock[SequencerApi]

    val eswDarkNightSequencer: ComponentId  = ComponentId(Prefix(ESW, darkNightObsMode.name), Sequencer)
    val eswClearSkiesSequencer: ComponentId = ComponentId(Prefix(ESW, clearSkiesObsMode.name), Sequencer)
    val tcsDarkNightSequencer: ComponentId  = ComponentId(Prefix(TCS, darkNightObsMode.name), Sequencer)
    val irisDarkNightSequencer: ComponentId = ComponentId(Prefix(IRIS, darkNightObsMode.name), Sequencer)

    val eswDarkNightSequencerLoc: AkkaLocation  = akkaLocation(eswDarkNightSequencer)
    val eswClearSkiesSequencerLoc: AkkaLocation = akkaLocation(eswClearSkiesSequencer)
    val tcsDarkNightSequencerLoc: AkkaLocation  = akkaLocation(tcsDarkNightSequencer)

    val eswPrimarySeqCompLoc: AkkaLocation   = akkaLocation(ComponentId(Prefix(ESW, "primary"), SequenceComponent))
    val eswSecondarySeqCompLoc: AkkaLocation = akkaLocation(ComponentId(Prefix(ESW, "secondary"), SequenceComponent))
    val tcsPrimarySeqCompLoc: AkkaLocation   = akkaLocation(ComponentId(Prefix(TCS, "primary"), SequenceComponent))
    val irisPrimarySeqCompLoc: AkkaLocation  = akkaLocation(ComponentId(Prefix(IRIS, "primary"), SequenceComponent))

    val masterSeqConnection: HttpConnection = HttpConnection(eswDarkNightSequencer)
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""))

    val sequencerUtil: SequencerUtil = new SequencerUtil(locationServiceUtil, sequenceComponentUtil) {
      override private[sm] def makeSequencerClient(sequencerLocation: Location) =
        sequencerLocation.prefix.subsystem match {
          case ESW => eswSequencerApi
          case TCS => tcsSequencerApi
          case _   => mock[SequencerApi]
        }
    }

    private def akkaLocation(componentId: ComponentId): AkkaLocation = AkkaLocation(AkkaConnection(componentId), URI.create(""))
  }
}
