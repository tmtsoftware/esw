package esw.sm.impl.utils

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.*
import csw.location.api.models.ComponentType.{SequenceComponent, Sequencer}
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.{ObsMode, SequencerId}
import esw.ocs.api.models.SequenceComponentState.Idle
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{Ok, SequencerLocation, Unhandled}
import esw.sm.api.models.SequencerIds
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{FailedToStartSequencers, Success}
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.api.protocol.{RestartSequencerResponse, ShutdownSequencersResponse}
import esw.testcommons.BaseTestSuite

import java.net.URI
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}

class SequencerUtilTest extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "testSystem")
  implicit val ec: ExecutionContext                       = system.executionContext
  implicit val timeout: Timeout                           = 5.seconds

  private val setup = new TestSetup()
  import setup.*

  override protected def afterEach(): Unit = {
    super.afterEach()
    reset(locationServiceUtil, sequenceComponentUtil, eswSequencerApi, tcsSequencerApi)
  }

  override def afterAll(): Unit = {
    system.terminate()
    system.whenTerminated.futureValue
  }

  "startSequencersByMapping" must {
    val eswSequencerPrefix = Prefix(ESW, darkNightObsMode.name)
    val tcsSequencerPrefix = Prefix(TCS, darkNightObsMode.name)
    "start all the given sequencers | ESW-178, ESW-561" in {
      when(sequenceComponentUtil.loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(eswDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))

      // returns success with master sequencer location after starting all the sequencers
      val response = sequencerUtil
        .startSequencersByMapping(
          darkNightObsMode,
          List((eswSequencerPrefix, eswPrimarySeqCompLoc), (tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        )
        .futureValue

      response should ===(Success(eswDarkNightSequencer))
      verify(sequenceComponentUtil).loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc)
    }

    "start all the given sequencers concurrently | ESW-178, ESW-561" in {
      when(sequenceComponentUtil.loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc))
        .thenReturn(future(1.seconds, Right(Started(eswDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        .thenReturn(future(1.seconds, Right(Started(tcsDarkNightSequencer))))

      // each load script call taking 1 second
      // waiting for 1 second (200 millis processing time) will ensure that loading script is happening concurrently
      val response = Await.result(
        sequencerUtil
          .startSequencersByMapping(
            darkNightObsMode,
            List((eswSequencerPrefix, eswPrimarySeqCompLoc), (tcsSequencerPrefix, tcsPrimarySeqCompLoc))
          ),
        1200.millis
      )

      response should ===(Success(eswDarkNightSequencer))
      verify(sequenceComponentUtil).loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc)
    }

    "return all the errors caused while starting the sequencers  | ESW-178, ESW-561" in {
      // load script error for ESW
      val loadScriptError = LoadScriptError("load script error")
      when(sequenceComponentUtil.loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc)).thenReturn(futureLeft(loadScriptError))
      when(sequenceComponentUtil.loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))

      val response = sequencerUtil
        .startSequencersByMapping(
          darkNightObsMode,
          List((eswSequencerPrefix, eswPrimarySeqCompLoc), (tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        )
        .futureValue

      response should ===(FailedToStartSequencers(Set(loadScriptError.msg)))
      verify(sequenceComponentUtil).loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc)
    }
  }

  "restartSequencer" must {
    "restart given sequencer that is running | ESW-327, ESW-561" in {
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.restartScript(eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(SequencerLocation(eswDarkNightSequencerLoc)))

      sequencerUtil.restartSequencer(eswDarkNightSequencerPrefix).futureValue should ===(
        RestartSequencerResponse.Success(eswDarkNightSequencer)
      )

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
      verify(sequenceComponentUtil).restartScript(eswPrimarySeqCompLoc)
    }

    "return LoadScriptError error if restart fails with LoadingScriptFailed | ESW-327, ESW-56" in {
      val errorMsg        = "loading script failed"
      val loadScriptError = LoadScriptError(errorMsg)
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.restartScript(eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(ScriptError.LoadingScriptFailed(errorMsg)))

      sequencerUtil.restartSequencer(eswDarkNightSequencerPrefix).futureValue should ===(loadScriptError)

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
      verify(sequenceComponentUtil).restartScript(eswPrimarySeqCompLoc)
    }

    "return LocationServiceError error if restart fails | ESW-327, ESW-56" in {
      val errorMsg = "location not found"
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix))
        .thenReturn(futureLeft(LocationNotFound("location not found")))

      sequencerUtil.restartSequencer(eswDarkNightSequencerPrefix).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
      verify(sequenceComponentUtil, never).restartScript(eswPrimarySeqCompLoc)
    }

    "return LoadScriptError error if restart fails with Unhandled| ESW-327, ESW-56" in {
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.restartScript(eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Unhandled(Idle, "Restart", "error")))

      sequencerUtil.restartSequencer(eswDarkNightSequencerPrefix).futureValue should ===(LoadScriptError("error"))

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
      verify(sequenceComponentUtil).restartScript(eswPrimarySeqCompLoc)
    }
  }

  "shutdownSequencer" must {
    "shutdown the given sequencer and return Done | ESW-326, ESW-351, ESW-56" in {
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix)).thenReturn(futureRight(eswDarkNightSequencerLoc))
      when(eswSequencerApi.getSequenceComponent).thenReturn(Future.successful(eswPrimarySeqCompLoc))
      when(sequenceComponentUtil.unloadScript(eswPrimarySeqCompLoc)).thenReturn(Future.successful(Ok))

      sequencerUtil.shutdownSequencer(eswDarkNightSequencerPrefix).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(eswSequencerApi).getSequenceComponent
      verify(sequenceComponentUtil).unloadScript(eswPrimarySeqCompLoc)
    }

    "return Success even if sequencer is not running | ESW-326, ESW-351, ESW-56" in {
      // mimic the exception thrown from LocationServiceUtil.findSequencer
      val findLocationFailed = futureLeft(LocationNotFound("location service error"))
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix)).thenReturn(findLocationFailed)

      sequencerUtil.shutdownSequencer(eswDarkNightSequencerPrefix).futureValue should ===(ShutdownSequencersResponse.Success)

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return Failure response when location service returns RegistrationListingFailed error | ESW-326, ESW-351, ESW-56" in {
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencer(eswDarkNightSequencerPrefix).futureValue should ===(LocationServiceError("Error"))

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
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
    val eswSequencerPrefix  = Prefix(ESW, darkNightObsMode.name)
    val tcsSequencerPrefix  = Prefix(TCS, darkNightObsMode.name)
    val irisSequencerPrefix = Prefix(IRIS, darkNightObsMode.name)
    val sequencerPrefixes   = List(irisSequencerPrefix, eswSequencerPrefix, tcsSequencerPrefix)
    val sequencerIds        = SequencerIds(SequencerId(IRIS), SequencerId(ESW), SequencerId(TCS))

    "return success when adequate idle sequence components are available and all sequencers are started successfully | ESW-178, ESW-561" in {
      when(sequenceComponentUtil.allocateSequenceComponents(sequencerPrefixes))
        .thenReturn(
          futureRight(
            List(
              (irisSequencerPrefix, eswPrimarySeqCompLoc),
              (eswSequencerPrefix, eswSecondarySeqCompLoc),
              (tcsSequencerPrefix, tcsPrimarySeqCompLoc)
            )
          )
        )
      when(sequenceComponentUtil.loadScript(eswSequencerPrefix, eswSecondarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(eswDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(irisSequencerPrefix, eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(irisDarkNightSequencer))))

      sequencerUtil.startSequencers(darkNightObsMode, sequencerIds).futureValue should ===(Success(eswDarkNightSequencer))

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencerPrefixes)
      verify(sequenceComponentUtil).loadScript(eswSequencerPrefix, eswSecondarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(irisSequencerPrefix, eswPrimarySeqCompLoc)
    }

    "return failure when adequate sequence components are not available to start sequencers | ESW-178, ESW-340, ESW-561" in {
      when(sequenceComponentUtil.allocateSequenceComponents(sequencerPrefixes))
        .thenReturn(futureLeft(SequenceComponentNotAvailable(List(eswSequencerPrefix))))

      sequencerUtil
        .startSequencers(darkNightObsMode, sequencerIds)
        .futureValue should ===(SequenceComponentNotAvailable(List(eswSequencerPrefix)))

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencerPrefixes)
    }

    "return failure when location service error | ESW-178, ESW-561" in {
      when(sequenceComponentUtil.allocateSequenceComponents(sequencerPrefixes))
        .thenReturn(Future.successful(Left(LocationServiceError("error"))))

      sequencerUtil.startSequencers(darkNightObsMode, sequencerIds).futureValue should ===(
        LocationServiceError("error")
      )

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencerPrefixes)
    }

    "return failure when load script fails | ESW-178, ESW-561" in {
      val eswLoadScriptError  = LoadScriptError("error for esw sequencer")
      val irisLoadScriptError = LoadScriptError("error for iris sequencer")

      when(sequenceComponentUtil.allocateSequenceComponents(sequencerPrefixes))
        .thenReturn(
          futureRight(
            List(
              (irisSequencerPrefix, irisPrimarySeqCompLoc),
              (eswSequencerPrefix, eswPrimarySeqCompLoc),
              (tcsSequencerPrefix, tcsPrimarySeqCompLoc)
            )
          )
        )
      when(sequenceComponentUtil.loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc))
        .thenReturn(Future.successful(Left(eswLoadScriptError)))
      when(sequenceComponentUtil.loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc))
        .thenReturn(Future.successful(Right(Started(tcsDarkNightSequencer))))
      when(sequenceComponentUtil.loadScript(irisSequencerPrefix, irisPrimarySeqCompLoc))
        .thenReturn(Future.successful(Left(irisLoadScriptError)))

      val actualError = sequencerUtil.startSequencers(darkNightObsMode, sequencerIds).futureValue
      actualError shouldBe a[FailedToStartSequencers]
      actualError.asInstanceOf[FailedToStartSequencers].msg should ===(
        s"Failed to configure: failed to start sequencers Set(${irisLoadScriptError.msg}, ${eswLoadScriptError.msg})"
      )

      verify(sequenceComponentUtil).allocateSequenceComponents(sequencerPrefixes)
      verify(sequenceComponentUtil).loadScript(eswSequencerPrefix, eswPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(tcsSequencerPrefix, tcsPrimarySeqCompLoc)
      verify(sequenceComponentUtil).loadScript(irisSequencerPrefix, irisPrimarySeqCompLoc)
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
    val masterSeqLocation: HttpLocation     = HttpLocation(masterSeqConnection, URI.create(""), Metadata.empty)

    val eswDarkNightSequencerPrefix: Prefix = Prefix(ESW, darkNightObsMode.name)

    val sequencerUtil: SequencerUtil = new SequencerUtil(locationServiceUtil, sequenceComponentUtil) {
      override private[sm] def makeSequencerClient(sequencerLocation: Location) =
        sequencerLocation.prefix.subsystem match {
          case ESW => eswSequencerApi
          case TCS => tcsSequencerApi
          case _   => mock[SequencerApi]
        }
    }

    private def akkaLocation(componentId: ComponentId): AkkaLocation =
      AkkaLocation(AkkaConnection(componentId), URI.create(""), Metadata.empty)
  }
}
