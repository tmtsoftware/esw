package esw.agent.app

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.Scheduler
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.TcpConnection
import csw.location.api.models.{ComponentId, TcpLocation, TcpRegistration}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.Killed._
import esw.agent.api._
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Random
import org.scalatest.matchers
import matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

//todo: fix test names
class KillManuallyRegisteredComponentTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with MockitoSugar
    with BeforeAndAfterEach {

  private val locationService = mock[LocationService]
  private val processExecutor = mock[ProcessExecutor]
  private val process         = mock[Process]
  private val logger          = mock[Logger]

  private val agentSettings         = AgentSettings("/tmp", 15.seconds, 3.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix                   = Prefix("csw.component")
  private val componentId: ComponentId = ComponentId(prefix, Service)
  private val redisConn                = TcpConnection(componentId)
  private val redisLocation            = TcpLocation(redisConn, new URI("some"))
  private val redisRegistration        = TcpRegistration(redisConn, 100)
  private val redisRegistrationResult  = RegistrationResult.from(redisLocation, con => locationService.unregister(con))
  private val spawnRedis               = SpawnRedis(_, prefix, 100, List.empty)

  "Kill (manually registered) Component" must {

    "reply 'killedGracefully' after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis()

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //wait it it is registered
      probe1.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped
      probe2.expectMessage(10.seconds, killedGracefully)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'killedForcefully' after stopping a registered component forcefully when it does not gracefully in given time | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForGracefulProcessTermination = 2.second))
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(5.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //wait it it is registered
      probe1.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped
      probe2.expectMessage(killedForcefully)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'killedGracefully' after killing a running component when component is waiting registration completion | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.hour)

      mockSuccessfulProcess(dieAfter = 3.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //it should not be registered
      probe1.expectNoMessage(2.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'killedForcefully' after killing a running component when component is waiting registration confirmation | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.hour)

      mockSuccessfulProcess(dieAfter = 20.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      //it should not be registered
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)

      //ensure it is stopped forcefully
      probe2.expectMessage(10.seconds, killedForcefully)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'killedGracefully', unregister the component and kill the component when registration is being performed | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.hour) //this will ensure actor remains in registering state
      mockSuccessfulProcess(2.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'Failed' when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForGracefulProcessTermination = 4.seconds))
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! spawnRedis(spawnProbe.ref)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, componentId)
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, componentId)

      //ensure it is stopped gracefully
      firstKiller.expectMessage(3.seconds, killedGracefully)
      secondKiller.expectMessage(Failed("process is already stopping"))

      //ensure component was unregistered
      verify(locationService).unregister(redisConn)
    }

    "reply 'Failed' when given component is not running on agent | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[KillResponse]()

      //try to stop the component
      agentActorRef ! KillComponent(probe.ref, ComponentId(Prefix("ESW.invalid"), Service))

      //verify that response is Failure
      probe.expectMessage(Failed("given component id is not running on this agent"))

      //ensure component was NOT unregistered
      verify(locationService, never).unregister(redisConn)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, process, logger)
  }

  private def mockSuccessfulProcess(dieAfter: FiniteDuration, exitCode: Int = 0) = {
    when(process.pid()).thenReturn(Random.nextInt(1000).abs)
    when(process.exitValue()).thenReturn(exitCode)
    val future = new CompletableFuture[Process]()
    scheduler.scheduleOnce(dieAfter, () => future.complete(process))
    when(process.onExit()).thenReturn(future)
    when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(process))
  }

  private def mockLocationServiceForRedis(registrationDuration: FiniteDuration = 0.seconds) = {
    when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
      .thenReturn(Future.successful(None))
    when(locationService.register(redisRegistration))
      .thenReturn(delayedFuture(redisRegistrationResult, registrationDuration))
  }

  private def spawnAgentActor(agentSettings: AgentSettings = agentSettings) = {
    spawn(new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty))
  }

  private def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    testKit.system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }
}
