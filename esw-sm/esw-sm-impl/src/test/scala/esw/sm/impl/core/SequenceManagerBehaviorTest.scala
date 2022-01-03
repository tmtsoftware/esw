package esw.sm.impl.core

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.*
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Metadata}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.*
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.{ObsMode, VariationInfo}
import esw.sm.api.actor.messages.SequenceManagerMsg.*
import esw.sm.api.actor.messages.{SequenceManagerMsg, UnhandleableSequenceManagerMsg}
import esw.sm.api.models.*
import esw.sm.api.models.ObsModeStatus.{Configurable, Configured, NonConfigurable}
import esw.sm.api.models.SequenceManagerState.{Idle, Processing}
import esw.sm.api.protocol.*
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{ConfigurationMissing, ConflictingResourcesWithRunningObsMode, Success}
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable, Started}
import esw.sm.impl.config.{ObsModeConfig, SequenceManagerConfig}
import esw.sm.impl.utils.{AgentUtil, SequenceComponentAllocator, SequenceComponentUtil, SequencerUtil}
import esw.testcommons.BaseTestSuite
import org.scalatest.prop.TableDrivenPropertyChecks

import java.net.URI
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import org.mockito.Mockito.{never, reset, times, verify, when}

class SequenceManagerBehaviorTest extends BaseTestSuite with TableDrivenPropertyChecks {

  private implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystem(SpawnProtocol(), "sequence-manager-system")

  private val loggerFactory: LoggerFactory = new LoggerFactory(Prefix("ESW.sequence_manager"))
  private implicit val logger: Logger      = loggerFactory.getLogger

  private val darkNight                              = ObsMode("DarkNight")
  private val clearSkies                             = ObsMode("ClearSkies")
  private val irisMCAO                               = ObsMode("IRIS_MCAO")
  private val randomObsMode                          = ObsMode("RandomObsMode")
  private val eswVariationId                         = VariationInfo(ESW)
  private val tcsVariationId                         = VariationInfo(TCS)
  private val wfosVariationId                        = VariationInfo(WFOS)
  private val darkNightVariationIds: VariationInfos  = VariationInfos(eswVariationId, tcsVariationId)
  private val clearSkiesVariationIds: VariationInfos = VariationInfos(eswVariationId)
  private val irisMCAOVariationIds: VariationInfos   = VariationInfos(eswVariationId, wfosVariationId)
  private val apsResource: Resource                  = Resource(APS)
  private val tcsResource: Resource                  = Resource(TCS)
  private val irisResource: Resource                 = Resource(IRIS)
  private val wfosResource: Resource                 = Resource(WFOS)
  private val config = SequenceManagerConfig(
    Map(
      darkNight  -> ObsModeConfig(Resources(apsResource, tcsResource), darkNightVariationIds),
      clearSkies -> ObsModeConfig(Resources(tcsResource, irisResource), clearSkiesVariationIds),
      irisMCAO   -> ObsModeConfig(Resources(wfosResource), irisMCAOVariationIds)
    )
  )

  private val locationServiceUtil: LocationServiceUtil               = mock[LocationServiceUtil]
  private val agentUtil: AgentUtil                                   = mock[AgentUtil]
  private val sequencerUtil: SequencerUtil                           = mock[SequencerUtil]
  private val sequenceComponentUtil: SequenceComponentUtil           = mock[SequenceComponentUtil]
  private val sequenceComponentAllocator: SequenceComponentAllocator = mock[SequenceComponentAllocator]
  private val sequenceManagerBehavior = new SequenceManagerBehavior(
    config,
    locationServiceUtil,
    agentUtil,
    sequencerUtil,
    sequenceComponentUtil
  )

  private lazy val smRef: ActorRef[SequenceManagerMsg] = actorSystem.systemActorOf(sequenceManagerBehavior.setup, "test_actor")

  override implicit def patienceConfig: PatienceConfig = PatienceConfig(10.seconds)

  override protected def beforeEach(): Unit = {
    reset(locationServiceUtil)
    reset(sequencerUtil)
    reset(sequenceComponentUtil)
    reset(agentUtil)
  }

  private def failedFuture(reason: String, delay: FiniteDuration) = {
    Thread.sleep(delay.toMillis)
    Future.failed(new Exception(reason))
  }

  "Sequence Manager " must {
    val eswDarkNightPrefix = Prefix(ESW, darkNight.name)
    "be able to handle next messages if the previous Provision call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [29000] ms"
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 2)
      when(sequenceComponentUtil.shutdownAllSequenceComponents())
        .thenReturn(future(500.millis, ShutdownSequenceComponentResponse.Success))

      when(agentUtil.provision(provisionConfig)).thenReturn(failedFuture(exceptionReason, 0.seconds))

      val provisionResponseProbe = TestProbe[ProvisionResponse]()

      assertState(Idle)
      smRef ! Provision(provisionConfig, provisionResponseProbe.ref)
      assertState(Processing)
      assertState(Idle)

      verify(agentUtil).provision(provisionConfig)
      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
      provisionResponseProbe.expectMessage(
        FailedResponse(s"Sequence Manager Operation(Provision) failed due to: $exceptionReason")
      )
    }

    "be able to handle next messages if the previous Configure call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [7000] ms"
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(darkNight, darkNightVariationIds))
        .thenReturn(failedFuture(exceptionReason, delay = 0.seconds))

      val configureProbe = TestProbe[ConfigureResponse]()
      assertState(Idle)
      smRef ! Configure(darkNight, configureProbe.ref)
      assertState(Processing)
      // goes back to idle after exception
      assertState(Idle)

      configureProbe.expectMessage(FailedResponse(s"Sequence Manager Operation(Configure) failed due to: $exceptionReason"))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(darkNight, darkNightVariationIds)
    }

    "be able to handle next messages if the previous StartSequencer call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [7000] ms"
      val connection      = HttpConnection(ComponentId(Prefix(ESW, darkNight.name), Sequencer))
      when(locationServiceUtil.find(connection)).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[StartSequencerResponse]()
      assertState(Idle)
      smRef ! StartSequencer(connection.prefix.subsystem, darkNight, None, testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(FailedResponse(s"Sequence Manager Operation(StartSequencer) failed due to: $exceptionReason"))
      verify(locationServiceUtil).find(connection)
    }

    "be able to handle next messages if the previous ShutdownSequencer call times-out due to downstream error | ESW-473, ESW-561" in {
      val exceptionReason = "Ask timed out after [10000] ms"
      when(sequencerUtil.shutdownSequencer(eswDarkNightPrefix)).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[ShutdownSequencersResponse]()
      assertState(Idle)
      smRef ! ShutdownSequencer(ESW, darkNight, None, testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(FailedResponse(s"Sequence Manager Operation(ShutdownSequencer) failed due to: $exceptionReason"))
      verify(sequencerUtil).shutdownSequencer(eswDarkNightPrefix)
    }

    "be able to handle next messages if the previous ShutdownSubsystemSequencers call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [10000] ms"
      when(sequencerUtil.shutdownSubsystemSequencers(ESW)).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[ShutdownSequencersResponse]()
      assertState(Idle)
      smRef ! ShutdownSubsystemSequencers(ESW, testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(
        FailedResponse(s"Sequence Manager Operation(ShutdownSubsystemSequencers) failed due to: $exceptionReason")
      )
      verify(sequencerUtil).shutdownSubsystemSequencers(ESW)
    }

    "be able to handle next messages if the previous ShutdownObsModeSequencers call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [10000] ms"
      when(sequencerUtil.shutdownObsModeSequencers(darkNight)).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[ShutdownSequencersResponse]()
      assertState(Idle)
      smRef ! ShutdownObsModeSequencers(darkNight, testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(
        FailedResponse(s"Sequence Manager Operation(ShutdownObsModeSequencers) failed due to: $exceptionReason")
      )
      verify(sequencerUtil).shutdownObsModeSequencers(darkNight)
    }

    "be able to handle next messages if the previous ShutdownAllSequencers call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [10000] ms"
      when(sequencerUtil.shutdownAllSequencers()).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[ShutdownSequencersResponse]()
      assertState(Idle)
      smRef ! ShutdownAllSequencers(testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(
        FailedResponse(s"Sequence Manager Operation(ShutdownAllSequencers) failed due to: $exceptionReason")
      )
      verify(sequencerUtil).shutdownAllSequencers()
    }

    "be able to handle next messages if the previous RestartSequencer call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [15000] ms"
      when(sequencerUtil.restartSequencer(eswDarkNightPrefix)).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[RestartSequencerResponse]()
      assertState(Idle)
      smRef ! RestartSequencer(ESW, darkNight, None, testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(FailedResponse(s"Sequence Manager Operation(RestartSequencer) failed due to: $exceptionReason"))
      verify(sequencerUtil).restartSequencer(eswDarkNightPrefix)
    }

    "be able to handle next messages if the previous ShutdownSequenceComponent call times-out due to downstream error | ESW-473" in {
      val prefix          = Prefix(ESW, "primary")
      val exceptionReason = "Ask timed out after [8000] ms"
      when(sequenceComponentUtil.shutdownSequenceComponent(prefix)).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[ShutdownSequenceComponentResponse]()
      assertState(Idle)
      smRef ! ShutdownSequenceComponent(prefix, testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(
        FailedResponse(s"Sequence Manager Operation(ShutdownSequenceComponent) failed due to: $exceptionReason")
      )
      verify(sequenceComponentUtil).shutdownSequenceComponent(prefix)
    }

    "be able to handle next messages if the previous ShutdownAllSequenceComponents call times-out due to downstream error | ESW-473" in {
      val exceptionReason = "Ask timed out after [8000] ms"
      when(sequenceComponentUtil.shutdownAllSequenceComponents()).thenReturn(failedFuture(exceptionReason, delay = 1.seconds))

      val testProbe = TestProbe[ShutdownSequenceComponentResponse]()
      assertState(Idle)
      smRef ! ShutdownAllSequenceComponents(testProbe.ref)
      // goes back to idle after exception
      assertState(Idle)

      testProbe.expectMessage(
        FailedResponse(s"Sequence Manager Operation(ShutdownAllSequenceComponents) failed due to: $exceptionReason")
      )
      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
    }
  }

  "Configure" must {

    "transition sm from Idle -> Processing -> Idle state and return location of master sequencer | ESW-164, ESW-178, ESW-342" in {
      val componentId    = ComponentId(Prefix(ESW, darkNight.name), Sequencer)
      val configResponse = Success(componentId)
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(1.seconds, Right(List.empty)))
      when(sequencerUtil.startSequencers(darkNight, darkNightVariationIds))
        .thenReturn(Future.successful(configResponse))
      val configureProbe = TestProbe[ConfigureResponse]()

      // STATE TRANSITION: Idle -> Configure() -> Processing -> Idle
      assertState(Idle)
      smRef ! Configure(darkNight, configureProbe.ref)
      assertState(Processing)
      assertState(Idle)

      configureProbe.expectMessage(configResponse)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil).startSequencers(darkNight, darkNightVariationIds)
    }

    "return LocationServiceError if location service fails to return running observation mode | ESW-164, ESW-178" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("Sequencer"))))

      val probe = TestProbe[ConfigureResponse]()
      smRef ! Configure(darkNight, probe.ref)

      probe.expectMessage(LocationServiceError("Sequencer"))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return ConflictingResourcesWithRunningObsMode when required resources are already in use | ESW-169, ESW-168, ESW-170, ESW-179, ESW-178" in {
      // this simulates that ClearSkies observation is running
      val location = akkaLocation(ComponentId(Prefix(ESW, clearSkies.name), Sequencer))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(location))))
      val probe = TestProbe[ConfigureResponse]()

      // r2 is a conflicting resource between DarkNight and ClearSkies observations
      smRef ! Configure(darkNight, probe.ref)

      probe.expectMessage(ConflictingResourcesWithRunningObsMode(Set(clearSkies)))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
      verify(sequencerUtil, times(0)).startSequencers(darkNight, darkNightVariationIds)
    }

    "return ConfigurationMissing error when config for given obsMode is missing | ESW-164, ESW-178" in {
      val location = akkaLocation(ComponentId(Prefix(ESW, randomObsMode.name), Sequencer))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(location))))
      val probe = TestProbe[ConfigureResponse]()

      smRef ! Configure(randomObsMode, probe.ref)

      probe.expectMessage(ConfigurationMissing(randomObsMode))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }
  }

  "StartSequencer" must {
    "transition sm from Idle -> Processing -> Idle state and return componentId of started sequencer | ESW-176, ESW-342, ESW-561" in {
      val componentId    = ComponentId(Prefix(ESW, darkNight.name), Sequencer)
      val httpConnection = HttpConnection(componentId)

      when(sequenceComponentUtil.loadScript(ESW, darkNight, None)).thenReturn(future(1.second, Started(componentId)))
      when(locationServiceUtil.find(httpConnection)).thenReturn(futureLeft(LocationNotFound("error")))

      val startSequencerResponseProbe = TestProbe[StartSequencerResponse]()

      // STATE TRANSITION: Idle -> StartSequencer -> Processing -> Idle
      assertState(Idle)
      smRef ! StartSequencer(ESW, darkNight, None, startSequencerResponseProbe.ref)
      assertState(Processing)
      assertState(Idle)

      startSequencerResponseProbe.expectMessage(StartSequencerResponse.Started(componentId))
      verify(sequenceComponentUtil).loadScript(ESW, darkNight, None)
      verify(locationServiceUtil).find(httpConnection)
    }

    "return AlreadyRunning if sequencer for given obs mode is already running | ESW-176, ESW-561" in {
      val componentId    = ComponentId(Prefix(ESW, darkNight.name), Sequencer)
      val httpConnection = HttpConnection(componentId)
      val httpLocation   = HttpLocation(httpConnection, new URI("uri"), Metadata.empty)

      when(locationServiceUtil.find(httpConnection))
        .thenReturn(futureRight(httpLocation))

      val startSequencerResponseProbe = TestProbe[StartSequencerResponse]()

      smRef ! StartSequencer(ESW, darkNight, None, startSequencerResponseProbe.ref)

      startSequencerResponseProbe.expectMessage(StartSequencerResponse.AlreadyRunning(componentId))
      verify(sequenceComponentUtil, never).loadScript(ESW, darkNight, None)
      verify(locationServiceUtil).find(httpConnection)
    }

    "return Error if start sequencer returns error | ESW-176, ESW-561" in {
      val componentId           = ComponentId(Prefix(ESW, darkNight.name), Sequencer)
      val httpConnection        = HttpConnection(componentId)
      val expectedErrorResponse = LoadScriptError("error")

      when(locationServiceUtil.find(httpConnection))
        .thenReturn(futureLeft(LocationNotFound("error")))
      when(sequenceComponentUtil.loadScript(ESW, darkNight, None)).thenReturn(Future.successful(expectedErrorResponse))

      val startSequencerResponseProbe = TestProbe[StartSequencerResponse]()

      smRef ! StartSequencer(ESW, darkNight, None, startSequencerResponseProbe.ref)

      startSequencerResponseProbe.expectMessage(expectedErrorResponse)
      verify(sequenceComponentUtil).loadScript(ESW, darkNight, None)
      verify(locationServiceUtil).find(httpConnection)
    }
  }

  "ShutdownSequencer" must {
    val responseProbe = TestProbe[ShutdownSequencersResponse]()
    val prefix        = Prefix(ESW, darkNight.name)
    val shutdownMsg   = ShutdownSequencer(ESW, darkNight, None, responseProbe.ref)
    s"transition sm from Idle -> Processing -> Idle state and stop| ESW-326, ESW-345, ESW-166, ESW-324, ESW-342, ESW-351, ESW-561" in {
      when(sequencerUtil.shutdownSequencer(prefix)).thenReturn(future(1.seconds, ShutdownSequencersResponse.Success))

      // STATE TRANSITION: Idle -> ShutdownSequencers -> Processing -> Idle
      assertState(Idle)
      smRef ! shutdownMsg
      assertState(Processing)
      assertState(Idle)

      responseProbe.expectMessage(ShutdownSequencersResponse.Success)
      verify(sequencerUtil).shutdownSequencer(prefix)
    }

    s"return LocationServiceError if location service fails | ESW-326, ESW-345, ESW-166, ESW-324, ESW-351, ESW-561" in {
      val err = LocationServiceError("error")
      when(sequencerUtil.shutdownSequencer(prefix)).thenReturn(Future.successful(err))

      smRef ! shutdownMsg
      responseProbe.expectMessage(err)

      verify(sequencerUtil).shutdownSequencer(prefix)
    }
  }

  "ShutdownSubsystemSequencers" must {
    val responseProbe = TestProbe[ShutdownSequencersResponse]()
    val shutdownMsg   = ShutdownSubsystemSequencers(ESW, responseProbe.ref)
    s"transition sm from Idle -> Processing -> Idle state and stop | ESW-326, ESW-345, ESW-166, ESW-324, ESW-342, ESW-351" in {
      when(sequencerUtil.shutdownSubsystemSequencers(ESW)).thenReturn(future(1.seconds, ShutdownSequencersResponse.Success))

      // STATE TRANSITION: Idle -> ShutdownSequencers -> Processing -> Idle
      assertState(Idle)
      smRef ! shutdownMsg
      assertState(Processing)
      assertState(Idle)

      responseProbe.expectMessage(ShutdownSequencersResponse.Success)
      verify(sequencerUtil).shutdownSubsystemSequencers(ESW)
    }

    s"return LocationServiceError if location service fails for | ESW-326, ESW-345, ESW-166, ESW-324, ESW-351" in {
      val err = LocationServiceError("error")
      when(sequencerUtil.shutdownSubsystemSequencers(ESW)).thenReturn(Future.successful(err))

      smRef ! shutdownMsg
      responseProbe.expectMessage(err)

      verify(sequencerUtil).shutdownSubsystemSequencers(ESW)
    }
  }

  "ShutdownObsModeSequencers" must {
    val responseProbe = TestProbe[ShutdownSequencersResponse]()
    val shutdownMsg   = ShutdownObsModeSequencers(darkNight, responseProbe.ref)
    s"transition sm from Idle -> Processing -> Idle state and stop | ESW-326, ESW-345, ESW-166, ESW-324, ESW-342, ESW-351" in {
      when(sequencerUtil.shutdownObsModeSequencers(darkNight)).thenReturn(future(1.seconds, ShutdownSequencersResponse.Success))

      // STATE TRANSITION: Idle -> ShutdownSequencers -> Processing -> Idle
      assertState(Idle)
      smRef ! shutdownMsg
      assertState(Processing)
      assertState(Idle)

      responseProbe.expectMessage(ShutdownSequencersResponse.Success)
      verify(sequencerUtil).shutdownObsModeSequencers(darkNight)
    }

    s"return LocationServiceError if location service fails | ESW-326, ESW-345, ESW-166, ESW-324, ESW-351" in {
      val err = LocationServiceError("error")
      when(sequencerUtil.shutdownObsModeSequencers(darkNight)).thenReturn(Future.successful(err))

      smRef ! shutdownMsg
      responseProbe.expectMessage(err)

      verify(sequencerUtil).shutdownObsModeSequencers(darkNight)
    }
  }

  "ShutdownAllSequencers" must {
    val responseProbe = TestProbe[ShutdownSequencersResponse]()
    val shutdownMsg   = ShutdownAllSequencers(responseProbe.ref)
    s"transition sm from Idle -> Processing -> Idle state and stop | ESW-326, ESW-345, ESW-166, ESW-324, ESW-342, ESW-351" in {
      when(sequencerUtil.shutdownAllSequencers()).thenReturn(future(1.seconds, ShutdownSequencersResponse.Success))

      // STATE TRANSITION: Idle -> ShutdownSequencers -> Processing -> Idle
      assertState(Idle)
      smRef ! shutdownMsg
      assertState(Processing)
      assertState(Idle)

      responseProbe.expectMessage(ShutdownSequencersResponse.Success)
      verify(sequencerUtil).shutdownAllSequencers()
    }

    s"return LocationServiceError if location service fails | ESW-326, ESW-345, ESW-166, ESW-324, ESW-351" in {
      val err = LocationServiceError("error")
      when(sequencerUtil.shutdownAllSequencers()).thenReturn(Future.successful(err))

      smRef ! shutdownMsg
      responseProbe.expectMessage(err)

      verify(sequencerUtil).shutdownAllSequencers()
    }
  }

  "RestartSequencer" must {
    "transition sm from Idle -> Processing -> Idle state and return success on restart | ESW-327, ESW-342, ESW-561" in {
      val prefix      = Prefix(ESW, darkNight.name)
      val componentId = ComponentId(prefix, Sequencer)

      when(sequencerUtil.restartSequencer(prefix))
        .thenReturn(future(1.seconds, RestartSequencerResponse.Success(componentId)))

      val restartSequencerResponseProbe = TestProbe[RestartSequencerResponse]()

      // STATE TRANSITION: Idle -> RestartSequencer -> Processing -> Idle
      assertState(Idle)
      smRef ! RestartSequencer(ESW, darkNight, None, restartSequencerResponseProbe.ref)
      assertState(Processing)
      assertState(Idle)

      restartSequencerResponseProbe.expectMessage(RestartSequencerResponse.Success(componentId))
      verify(sequencerUtil).restartSequencer(prefix)
    }

    val errors = Table(
      ("errorName", "error", "process"),
      ("LocationServiceError", LocationServiceError("location service error"), "stop"),
      ("LoadScriptError", LoadScriptError("load script failed"), "start")
    )

    forAll(errors) { (errorName, error, process) =>
      s"return $errorName if $errorName encountered while sequencer $process | ESW-327, ESW-561" in {
        val prefix = Prefix(ESW, darkNight.name)
        when(sequencerUtil.restartSequencer(prefix))
          .thenReturn(future(1.seconds, error))

        val restartSequencerResponseProbe = TestProbe[RestartSequencerResponse]()

        smRef ! RestartSequencer(ESW, darkNight, None, restartSequencerResponseProbe.ref)
        restartSequencerResponseProbe.expectMessage(error)

        verify(sequencerUtil).restartSequencer(prefix)
      }
    }
  }

  "ShutdownSequenceComponents" must {
    "transition sm from Idle -> Processing -> Idle state and return success on shutdown | ESW-338, ESW-342, ESW-351" in {
      val prefix = Prefix(ESW, "primary")

      when(sequenceComponentUtil.shutdownSequenceComponent(prefix))
        .thenReturn(future(1.second, ShutdownSequenceComponentResponse.Success))

      val shutdownSequenceComponentResponseProbe = TestProbe[ShutdownSequenceComponentResponse]()

      // STATE TRANSITION: Idle -> ShutdownSequenceComponents -> Processing -> Idle
      assertState(Idle)
      smRef ! ShutdownSequenceComponent(prefix, shutdownSequenceComponentResponseProbe.ref)
      assertState(Processing)
      assertState(Idle)

      shutdownSequenceComponentResponseProbe.expectMessage(ShutdownSequenceComponentResponse.Success)
      verify(sequenceComponentUtil).shutdownSequenceComponent(prefix)
    }

    "return Success when shutting down all sequence components | ESW-346, ESW-351" in {
      when(sequenceComponentUtil.shutdownAllSequenceComponents())
        .thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))

      val shutdownSequenceComponentResponseProbe = TestProbe[ShutdownSequenceComponentResponse]()

      smRef ! ShutdownAllSequenceComponents(shutdownSequenceComponentResponseProbe.ref)
      shutdownSequenceComponentResponseProbe.expectMessage(ShutdownSequenceComponentResponse.Success)

      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
    }

    "return LocationServiceError if LocationServiceError encountered while shutting down all sequence components | ESW-346, ESW-351" in {
      when(sequenceComponentUtil.shutdownAllSequenceComponents())
        .thenReturn(Future.successful(LocationServiceError("error")))

      val shutdownSequenceComponentResponseProbe = TestProbe[ShutdownSequenceComponentResponse]()

      smRef ! ShutdownAllSequenceComponents(shutdownSequenceComponentResponseProbe.ref)
      shutdownSequenceComponentResponseProbe.expectMessage(LocationServiceError("error"))

      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
    }

    "return LocationServiceError if LocationServiceError encountered while shutting down sequence components | ESW-338,ESW-346, ESW-351" in {
      val prefix = Prefix(ESW, "primary")
      val error  = LocationServiceError("location service error")

      when(sequenceComponentUtil.shutdownSequenceComponent(prefix)).thenReturn(Future.successful(error))
      val shutdownSequenceComponentResponseProbe = TestProbe[ShutdownSequenceComponentResponse]()

      smRef ! ShutdownSequenceComponent(prefix, shutdownSequenceComponentResponseProbe.ref)
      shutdownSequenceComponentResponseProbe.expectMessage(error)

      verify(sequenceComponentUtil).shutdownSequenceComponent(prefix)
    }
  }

  "Provision" must {
    "transition from Idle -> Processing -> Idle and return provision success | ESW-347" in {
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 2)
      when(sequenceComponentUtil.shutdownAllSequenceComponents())
        .thenReturn(future(500.millis, ShutdownSequenceComponentResponse.Success))
      when(agentUtil.provision(provisionConfig)).thenReturn(future(500.millis, ProvisionResponse.Success))
      val provisionResponseProbe = TestProbe[ProvisionResponse]()

      assertState(Idle)
      smRef ! Provision(provisionConfig, provisionResponseProbe.ref)
      assertState(Processing)
      assertState(Idle)

      verify(agentUtil).provision(provisionConfig)
      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
      provisionResponseProbe.expectMessage(ProvisionResponse.Success)
    }

    "shutdown all running sequence components and return ProvisionResponse given by agentUtil.provision | ESW-347, ESW-358" in {
      val provisionConfig   = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 2)
      val provisionResponse = ProvisionResponse.CouldNotFindMachines(Set(Prefix(ESW, "primary")))
      when(sequenceComponentUtil.shutdownAllSequenceComponents())
        .thenReturn(Future.successful(ShutdownSequenceComponentResponse.Success))
      when(agentUtil.provision(provisionConfig)).thenReturn(Future.successful(provisionResponse))

      val provisionResponseProbe = TestProbe[ProvisionResponse]()
      smRef ! Provision(provisionConfig, provisionResponseProbe.ref)
      assertState(Idle)
      provisionResponseProbe.expectMessage(provisionResponse)

      verify(agentUtil).provision(provisionConfig)
      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
    }

    "return error caused while shutting down all sequence components | ESW-358" in {
      val provisionConfig  = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 2)
      val shutdownResponse = LocationServiceError("error")
      when(sequenceComponentUtil.shutdownAllSequenceComponents()).thenReturn(Future.successful(shutdownResponse))

      val provisionResponseProbe = TestProbe[ProvisionResponse]()
      smRef ! Provision(provisionConfig, provisionResponseProbe.ref)
      assertState(Idle)
      provisionResponseProbe.expectMessage(shutdownResponse)

      verify(sequenceComponentUtil).shutdownAllSequenceComponents()
    }
  }

  "Processing -> Unhandled" must {
    "return Unhandled if msg is unhandled in processing state | ESW-349, ESW-561" in {
      // hold configure completion by delay of 60 seconds. So SM will remain in processing state
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(future(60.seconds, Right(List.empty)))

      val configureProbe  = TestProbe[ConfigureResponse]()
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 2, Prefix(IRIS, "primary") -> 2)

      assertState(Idle)
      smRef ! Configure(darkNight, configureProbe.ref)

      // assert that SM is in Processing state
      assertState(Processing)

      // Assert that following msgs are getting unhandled response back in processing state
      assertUnhandled(
        state = Processing,
        ShutdownAllSequencers,
        Configure(clearSkies, _),
        ShutdownSequencer(ESW, darkNight, None, _),
        ShutdownObsModeSequencers(clearSkies, _),
        ShutdownSubsystemSequencers(ESW, _),
        StartSequencer(ESW, darkNight, None, _),
        RestartSequencer(ESW, darkNight, None, _),
        ShutdownSequenceComponent(Prefix(ESW, "primary"), _),
        ShutdownAllSequenceComponents,
        Provision(provisionConfig, _)
      )
    }
  }

  "GetObsModesDetails" must {
    "return LocationServiceError if location service fails to return running observation mode | ESW-466" in {
      val errorMessage = "Sequencer not found"
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(errorMessage))))

      val probe = TestProbe[ObsModesDetailsResponse]()

      smRef ! GetObsModesDetails(probe.ref)

      probe.expectMessage(LocationServiceError(errorMessage))
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return set of Observer modes that are all configurable if location service returns empty list | ESW-466, ESW-561" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Right(List())))

      val idleSeqComps = List.empty
      when(sequenceComponentUtil.getAllIdleSequenceComponents).thenReturn(Future.successful(Right(idleSeqComps)))
      when(sequenceComponentUtil.sequenceComponentAllocator).thenReturn(sequenceComponentAllocator)
      List((clearSkiesVariationIds, clearSkies), (darkNightVariationIds, darkNight), (irisMCAOVariationIds, irisMCAO)).foreach(
        tuple =>
          when(sequenceComponentAllocator.allocate(idleSeqComps, tuple._2, tuple._1.variationInfos)).thenReturn(Right(List.empty))
      )

      val probe = TestProbe[ObsModesDetailsResponse]()

      smRef ! GetObsModesDetails(probe.ref)

      val expectedMessage = ObsModesDetailsResponse.Success(
        Set(
          ObsModeDetails(clearSkies, Configurable, Resources(tcsResource, irisResource), clearSkiesVariationIds),
          ObsModeDetails(darkNight, Configurable, Resources(apsResource, tcsResource), darkNightVariationIds),
          ObsModeDetails(irisMCAO, Configurable, Resources(wfosResource), irisMCAOVariationIds)
        )
      )
      probe.expectMessage(expectedMessage)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }

    "return set of Observation modes with their respective statuses | ESW-466" in {
      val location = akkaLocation(ComponentId(Prefix(ESW, clearSkies.name), Sequencer))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List(location))))

      val idleSeqComps = List.empty
      when(sequenceComponentUtil.getAllIdleSequenceComponents).thenReturn(Future.successful(Right(idleSeqComps)))
      when(sequenceComponentUtil.sequenceComponentAllocator).thenReturn(sequenceComponentAllocator)
      List((clearSkiesVariationIds, clearSkies), (darkNightVariationIds, darkNight), (irisMCAOVariationIds, irisMCAO)).foreach(
        tuple =>
          when(sequenceComponentAllocator.allocate(idleSeqComps, tuple._2, tuple._1.variationInfos)).thenReturn(Right(List.empty))
      )
      val obsModesDetailsResponseProbe = TestProbe[ObsModesDetailsResponse]()
      smRef ! GetObsModesDetails(obsModesDetailsResponseProbe.ref)

      val expectedMessage = ObsModesDetailsResponse.Success(
        Set(
          ObsModeDetails(clearSkies, Configured, Resources(tcsResource, irisResource), clearSkiesVariationIds),
          ObsModeDetails(
            darkNight,
            NonConfigurable(VariationInfos.empty),
            Resources(apsResource, tcsResource),
            darkNightVariationIds
          ),
          ObsModeDetails(irisMCAO, Configurable, Resources(wfosResource), irisMCAOVariationIds)
        )
      )
      obsModesDetailsResponseProbe.expectMessage(expectedMessage)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)

    }

    "return set of Observation modes with non-configurable when sequence component are  missing | ESW-529, ESW-561" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List.empty)))

      val idleSeqComps = List.empty
      when(sequenceComponentUtil.getAllIdleSequenceComponents).thenReturn(Future.successful(Right(idleSeqComps)))
      when(sequenceComponentUtil.sequenceComponentAllocator).thenReturn(sequenceComponentAllocator)
      when(sequenceComponentAllocator.allocate(idleSeqComps, darkNight, darkNightVariationIds.variationInfos))
        .thenReturn(Left(SequenceComponentNotAvailable(VariationInfos(tcsVariationId))))

      List((clearSkiesVariationIds, clearSkies), (irisMCAOVariationIds, irisMCAO)).foreach(tuple =>
        when(sequenceComponentAllocator.allocate(idleSeqComps, tuple._2, tuple._1.variationInfos)).thenReturn(Right(List.empty))
      )

      val obsModesDetailsResponseProbe = TestProbe[ObsModesDetailsResponse]()
      smRef ! GetObsModesDetails(obsModesDetailsResponseProbe.ref)

      val expectedMessage = ObsModesDetailsResponse.Success(
        Set(
          ObsModeDetails(clearSkies, Configurable, Resources(tcsResource, irisResource), clearSkiesVariationIds),
          ObsModeDetails(
            darkNight,
            NonConfigurable(VariationInfos(tcsVariationId)),
            Resources(apsResource, tcsResource),
            darkNightVariationIds
          ),
          ObsModeDetails(irisMCAO, Configurable, Resources(wfosResource), irisMCAOVariationIds)
        )
      )
      obsModesDetailsResponseProbe.expectMessage(expectedMessage)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Sequencer)
    }
  }

  "Get Resources" must {

    "return all available resources when no obsMode is running | ESW-467 " in {
      val getResponseProbe = TestProbe[ResourcesStatusResponse]()

      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(List.empty)))

      val expectedResources =
        Set(
          ResourceStatusResponse(apsResource),
          ResourceStatusResponse(tcsResource),
          ResourceStatusResponse(irisResource),
          ResourceStatusResponse(wfosResource)
        )

      smRef ! GetResources(getResponseProbe.ref)
      val res = getResponseProbe.expectMessageType[ResourcesStatusResponse.Success]
      res.resourcesStatus.toSet should ===(expectedResources)
    }

    "return resources in use when obsMode is configured | ESW-467 " in {
      val getResponseProbe = TestProbe[ResourcesStatusResponse]()
      val locations        = List(akkaLocation(ComponentId(Prefix(ESW, darkNight.name), Sequencer)))
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer)).thenReturn(Future.successful(Right(locations)))

      val expectedResources =
        Set(
          ResourceStatusResponse(apsResource, ResourceStatus.InUse, Some(darkNight)),
          ResourceStatusResponse(tcsResource, ResourceStatus.InUse, Some(darkNight)),
          ResourceStatusResponse(irisResource),
          ResourceStatusResponse(wfosResource)
        )

      smRef ! GetResources(getResponseProbe.ref)
      val res = getResponseProbe.expectMessageType[ResourcesStatusResponse.Success]
      res.resourcesStatus.toSet should ===(expectedResources)
    }

    "return Failed if RegistrationListingFailed gives error | ESW-467" in {
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer))
        .thenReturn(Future.successful(Left(RegistrationListingFailed("error"))))

      val getResponseProbe = TestProbe[ResourcesStatusResponse]()

      smRef ! GetResources(getResponseProbe.ref)

      getResponseProbe.expectMessage(ResourcesStatusResponse.Failed("error"))
    }

  }

  private def assertUnhandled[T >: Unhandled <: SmResponse](
      state: SequenceManagerState,
      msg: ActorRef[T] => UnhandleableSequenceManagerMsg
  ): Unit = {
    val probe                  = TestProbe[T]()
    val sequenceManagerMessage = msg(probe.ref)
    smRef ! sequenceManagerMessage
    probe.expectMessage(Unhandled(state.entryName, sequenceManagerMessage.getClass.getSimpleName))
  }

  private def assertUnhandled[T >: Unhandled <: SmResponse](
      state: SequenceManagerState,
      msgs: (ActorRef[T] => UnhandleableSequenceManagerMsg)*
  ): Unit =
    msgs.foreach(assertUnhandled(state, _))

  private def assertState(state: SequenceManagerState) = {
    val stateProbe = TestProbe[SequenceManagerState]()
    eventually {
      smRef ! GetSequenceManagerState(stateProbe.ref)
      stateProbe.expectMessage(state)
    }
  }

  private def akkaLocation(componentId: ComponentId) =
    AkkaLocation(AkkaConnection(componentId), URI.create("uri"), Metadata.empty)
}
