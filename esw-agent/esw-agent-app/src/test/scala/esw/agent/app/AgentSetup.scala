package esw.agent.app

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.Done
import akka.actor.typed.{ActorRef, ActorSystem, Scheduler, SpawnProtocol}
import csw.location.api.models.ComponentType.{SequenceComponent, Service}
import csw.location.api.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, TcpLocation, TcpRegistration}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.{AgentCommand, SpawnResponse}
import esw.agent.app.process.{ProcessExecutor, ProcessManager}
import org.mockito.ArgumentMatchers.{any, eq => argEq}

import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Random

class AgentSetup extends BaseTestSuite {
  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  implicit val scheduler: Scheduler                       = system.scheduler
  implicit val ec: ExecutionContext                       = system.executionContext

  val locationService: LocationService = mock[LocationService]
  val processExecutor: ProcessExecutor = mock[ProcessExecutor]
  val processManager: ProcessManager   = mock[ProcessManager]
  val process: Process                 = mock[Process]
  val processHandle: ProcessHandle     = mock[ProcessHandle]
  val logger: Logger                   = mock[Logger]
  val agentSettings: AgentSettings     = AgentSettings(15.seconds, Cs.channel)

  val prefix: Prefix                                    = Prefix("csw.component")
  val componentId: ComponentId                          = ComponentId(prefix, Service)
  val redisConn: TcpConnection                          = TcpConnection(componentId)
  val redisLocation: TcpLocation                        = TcpLocation(redisConn, new URI("some"))
  val redisLocationF: Future[Some[TcpLocation]]         = Future.successful(Some(redisLocation))
  val redisRegistration: TcpRegistration                = TcpRegistration(redisConn, 100)
  val spawnRedis: ActorRef[SpawnResponse] => SpawnRedis = SpawnRedis(_, prefix, 100, List.empty)

  val seqCompPrefix: Prefix                        = Prefix("csw.component")
  val seqCompComponentId: ComponentId              = ComponentId(seqCompPrefix, SequenceComponent)
  val seqCompConn: AkkaConnection                  = AkkaConnection(seqCompComponentId)
  val seqCompLocation: AkkaLocation                = AkkaLocation(seqCompConn, new URI("some"))
  val seqCompLocationF: Future[Some[AkkaLocation]] = Future.successful(Some(seqCompLocation))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, process, logger)
  }

  def spawnAgentActor(agentSettings: AgentSettings = agentSettings, name: String = "test-actor"): ActorRef[AgentCommand] =
    system.systemActorOf(
      new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty),
      name
    )

  def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }

  def mockSuccessfulProcess(dieAfter: FiniteDuration = 2.seconds, exitCode: Int = 0): Unit = {
    when(process.pid()).thenReturn(Random.nextInt(1000).abs)
    when(process.toHandle).thenReturn(processHandle)
    when(process.exitValue()).thenReturn(exitCode)
    when(process.isAlive).thenReturn(true)
    val future = new CompletableFuture[Process]()
    scheduler.scheduleOnce(dieAfter, () => future.complete(process))
    when(process.onExit()).thenReturn(future)

    val future2 = new CompletableFuture[ProcessHandle]()
    scheduler.scheduleOnce(dieAfter, () => future2.complete(processHandle))
    when(processHandle.onExit()).thenReturn(future2)
    when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(process))
  }

  def mockLocationServiceForRedis(registrationDuration: FiniteDuration = 0.seconds): Unit = {
    when(locationService.resolve(argEq(redisConn), any[FiniteDuration])).thenReturn(Future.successful(None))
    when(locationService.register(redisRegistration)).thenReturn(
      delayedFuture(RegistrationResult.from(redisLocation, locationService.unregister), registrationDuration)
    )
    when(locationService.unregister(redisConn)).thenReturn(Future.successful(Done))
  }
}
