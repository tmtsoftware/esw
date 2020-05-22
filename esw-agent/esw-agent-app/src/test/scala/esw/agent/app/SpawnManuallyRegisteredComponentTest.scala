package esw.agent.app

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, TcpLocation, TcpRegistration}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api._
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Random

class SpawnManuallyRegisteredComponentTest extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "component-system")
  private val locationService                                     = mock[LocationService]
  private val processExecutor                                     = mock[ProcessExecutor]
  private val process                                             = mock[Process]
  private val logger                                              = mock[Logger]

  private val agentSettings         = AgentSettings("/tmp", 15.seconds, 3.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix = Prefix("csw.component")

  private val componentId: ComponentId = ComponentId(prefix, Service)
  private val redisConn                = TcpConnection(componentId)
  private val redisLocation            = TcpLocation(redisConn, new URI("some"))
  private val redisLocationF           = Future.successful(Some(redisLocation))
  private val redisRegistration        = TcpRegistration(redisConn, 100)
  private val spawnRedis               = SpawnRedis(_, prefix, 100, List.empty)

  "SpawnManuallyRegistered (component)" must {

    "reply 'Spawned' and spawn component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Spawned)

      //ensure component is registered
      verify(locationService).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when `resolve` call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.failed(new RuntimeException("call failed")))

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("error occurred while resolving a component with location service"))

      //ensure component is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(redisLocationF)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("can not spawn component when it is already registered in location service"))

      //ensure component is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor4")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe1.ref)
      agentActorRef ! spawnRedis(probe2.ref)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed("given component is already in process"))

      //ensure redis is registered once
      verify(locationService).register(redisRegistration)
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()

      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("failure"))

      //ensure register is NOT registered
      verify(locationService, never).register(redisRegistration)
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))

      when(locationService.register(redisRegistration))
        .thenReturn(Future.failed(new RuntimeException("failure")))

      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("registration encountered an issue or timed out"))

      //ensure component is registered
      verify(locationService).register(redisRegistration)
    }

    "Unregister when process is spawned but exits before registration and registration is later succeeded | ESW-237" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForComponentRegistration = 4.seconds), "test-actor7")
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis(registrationDuration = 2.seconds)
      mockSuccessfulProcess(dieAfter = 500.millis)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(3.seconds, Failed("process died before registration"))

      //ensure component is registered
      verify(locationService).register(redisRegistration)

      //ensure component is unregistered later
      verify(locationService, timeout(1000)).unregister(redisConn)
    }

    "reply 'Failed' when spawning is aborted by another message | ESW-237, ESW-276" in {
      val agentActorRef = spawnAgentActor(
        agentSettings.copy(
          durationToWaitForComponentRegistration = 7.seconds,
          durationToWaitForGracefulProcessTermination = 7.seconds
        ),
        "test-actor8"
      )
      val spawner = TestProbe[SpawnResponse]()
      val killer  = TestProbe[KillResponse]()

      mockLocationServiceForRedis(registrationDuration = 5.seconds)
      mockSuccessfulProcess(dieAfter = 2.seconds)

      agentActorRef ! spawnRedis(spawner.ref)
      Thread.sleep(500)
      agentActorRef ! KillComponent(killer.ref, componentId)

      spawner.expectMessage(Failed("Aborted"))
      killer.expectMessage(Killed.gracefully)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, process, logger)
  }

  private def mockSuccessfulProcess(dieAfter: FiniteDuration = 2.seconds, exitCode: Int = 0) = {
    when(process.pid()).thenReturn(Random.nextInt(1000).abs)
    when(process.exitValue()).thenReturn(exitCode)
    val future = new CompletableFuture[Process]()
    scheduler.scheduleOnce(dieAfter, () => future.complete(process))
    when(process.onExit()).thenReturn(future)
    when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(process))
  }

  private def spawnAgentActor(agentSettings: AgentSettings = agentSettings, name: String) = {
    system.systemActorOf(new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty), name)
  }

  private def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }

  private def mockLocationServiceForRedis(registrationDuration: FiniteDuration = 0.seconds) = {
    when(locationService.resolve(argEq(redisConn), any[FiniteDuration])).thenReturn(Future.successful(None))
    when(locationService.register(redisRegistration)).thenReturn(
      delayedFuture(RegistrationResult.from(redisLocation, con => locationService.unregister(con)), registrationDuration)
    )
  }
}
