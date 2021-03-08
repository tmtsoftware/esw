package esw.agent.akka.app

import akka.actor.typed.{ActorRef, ActorSystem, Scheduler, SpawnProtocol}
import csw.config.client.commons.ConfigUtils
import csw.location.api.models.ComponentType.{Container, HCD, SequenceComponent, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.akka.app.process.{ProcessExecutor, ProcessManager}
import esw.agent.akka.client.AgentCommand
import esw.agent.akka.client.AgentCommand.SpawnCommand.{SpawnContainer, SpawnSequenceComponent, SpawnSequenceManager}
import esw.agent.akka.client.models.{ConfigFileLocation, ContainerConfig, ContainerMode}
import esw.agent.service.api.models.SpawnResponse
import esw.commons.utils.config.VersionManager
import esw.testcommons.BaseTestSuite
import org.mockito.ArgumentMatchers.{any, eq => argEq}

import java.net.URI
import java.nio.file.{Path, Paths}
import java.util.concurrent.CompletableFuture
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

class AgentSetup extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  implicit val scheduler: Scheduler                       = system.scheduler
  implicit val ec: ExecutionContext                       = system.executionContext

  val locationService: LocationService   = mock[LocationService]
  val configUtils: ConfigUtils           = mock[ConfigUtils]
  val versionManager: VersionManager     = mock[VersionManager]
  val processExecutor: ProcessExecutor   = mock[ProcessExecutor]
  val process: Process                   = mock[Process]
  val mockedProcessHandle: ProcessHandle = mock[ProcessHandle]
  implicit val logger: Logger            = mock[Logger]
  val agentPrefix: Prefix                = Prefix(randomSubsystem, randomString(10))
  val versionConfPath: Path              = Path.of(randomString(20))
  val agentSettings: AgentSettings       = AgentSettings(agentPrefix, Cs.channel, versionConfPath)
  val metadata: Metadata                 = Metadata().withAgentPrefix(agentPrefix).withPid(12345)

  val seqCompName: String                          = randomString(10)
  val seqCompPrefix: Prefix                        = Prefix(agentPrefix.subsystem, seqCompName)
  val seqCompComponentId: ComponentId              = ComponentId(seqCompPrefix, SequenceComponent)
  val seqCompConn: AkkaConnection                  = AkkaConnection(seqCompComponentId)
  val seqCompLocation: AkkaLocation                = AkkaLocation(seqCompConn, new URI("some"), metadata)
  val seqCompLocationF: Future[Some[AkkaLocation]] = Future.successful(Some(seqCompLocation))
  val spawnSequenceComp: ActorRef[SpawnResponse] => SpawnSequenceComponent =
    SpawnSequenceComponent(_, agentPrefix, seqCompName, None)

  val seqManagerPrefix: Prefix                        = Prefix("esw.sequence_manager")
  val seqManagerComponentId: ComponentId              = ComponentId(seqManagerPrefix, Service)
  val seqManagerConn: AkkaConnection                  = AkkaConnection(seqManagerComponentId)
  val seqManagerLocation: AkkaLocation                = AkkaLocation(seqManagerConn, new URI("some"), metadata)
  val seqManagerLocationF: Future[Some[AkkaLocation]] = Future.successful(Some(seqManagerLocation))
  val spawnSequenceManager: ActorRef[SpawnResponse] => SpawnSequenceManager =
    SpawnSequenceManager(_, Paths.get("obsmode.conf"), isConfigLocal = true, None)

  val containerConfig: ContainerConfig =
    ContainerConfig(
      "com.github.tmtsoftware.sample",
      "csw-sampledeploy",
      "SampleContainerCmdApp",
      "0.0.1",
      ContainerMode.Standalone,
      Path.of("container.conf"),
      ConfigFileLocation.Local
    )
  val containerPrefixOne: Prefix                                = Prefix(Subsystem.Container, "testContainer1")
  val containerComponentIdOne: ComponentId                      = ComponentId(containerPrefixOne, Container)
  val spawnContainer: ActorRef[SpawnResponse] => SpawnContainer = SpawnContainer(_, containerComponentIdOne, containerConfig)
  val containerConnOne: AkkaConnection                          = AkkaConnection(containerComponentIdOne)
  val containerLocationOne: AkkaLocation                        = AkkaLocation(containerConnOne, new URI("some"), metadata)
  val containerLocationOneF: Future[Some[AkkaLocation]]         = Future.successful(Some(containerLocationOne))
  val componentPrefixTwo: Prefix                                = Prefix(ESW, "testHCD")
  val componentCompIdTwo: ComponentId                           = ComponentId(componentPrefixTwo, HCD)
  val containerConnTwo: AkkaConnection                          = AkkaConnection(componentCompIdTwo)
  val containerLocationTwo: AkkaLocation                        = AkkaLocation(containerConnTwo, new URI("some"), metadata)
  val containerLocationTwoF: Future[Some[AkkaLocation]]         = Future.successful(Some(containerLocationTwo))

  val sequencerScriptsVersion: String = randomString(10)
  val eswVersion: String              = randomString(10)

  when(versionManager.getScriptVersion).thenReturn(Future.successful(sequencerScriptsVersion))
  when(versionManager.eswVersion).thenReturn(Future.successful(eswVersion))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, process, logger, configUtils)
  }

  def spawnAgentActor(
      agentSettings: AgentSettings = agentSettings,
      name: String = "test-actor",
      hostConfigPath: Option[String] = None,
      isConfigLocal: Boolean = true
  ): ActorRef[AgentCommand] = {
    val processManager: ProcessManager = new ProcessManager(locationService, versionManager, processExecutor, agentSettings) {
      override def processHandle(pid: Long): Option[ProcessHandle] = Some(mockedProcessHandle)
    }
    system.systemActorOf(new AgentActor(processManager, configUtils, hostConfigPath, isConfigLocal).behavior, name)
  }

  def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }

  def mockSuccessfulProcess(dieAfter: FiniteDuration = 2.seconds, exitCode: Int = 0): Unit = {
    when(process.pid()).thenReturn(Random.nextInt(1000).abs)
    when(process.toHandle).thenReturn(mockedProcessHandle)
    when(process.exitValue()).thenReturn(exitCode)
    when(process.isAlive).thenReturn(true)
    val future = new CompletableFuture[Process]()
    scheduler.scheduleOnce(dieAfter, () => future.complete(process))
    when(process.onExit()).thenReturn(future)

    val future2 = new CompletableFuture[ProcessHandle]()
    scheduler.scheduleOnce(dieAfter, () => future2.complete(mockedProcessHandle))
    when(mockedProcessHandle.onExit()).thenReturn(future2)
    when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(process))
  }

  def mockLocationService(): Unit = {
    // Sequence Component
    when(locationService.find(argEq(seqCompConn))).thenReturn(Future.successful(None))
    when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration])).thenReturn(seqCompLocationF)

    // Sequence Manager
    when(locationService.find(argEq(seqManagerConn))).thenReturn(Future.successful(None))
    when(locationService.resolve(argEq(seqManagerConn), any[FiniteDuration])).thenReturn(seqManagerLocationF)

    // Container
    when(locationService.find(argEq(containerConnOne))).thenReturn(Future.successful(None))
    when(locationService.resolve(argEq(containerConnOne), any[FiniteDuration])).thenReturn(containerLocationOneF)
    when(locationService.find(argEq(containerConnTwo))).thenReturn(Future.successful(None))
    when(locationService.resolve(argEq(containerConnTwo), any[FiniteDuration])).thenReturn(containerLocationTwoF)
  }
}
