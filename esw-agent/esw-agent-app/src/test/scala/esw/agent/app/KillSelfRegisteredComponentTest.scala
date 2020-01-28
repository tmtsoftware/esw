package esw.agent.app

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.Scheduler
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType.SequenceComponent
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.Killed._
import esw.agent.api._
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.matchers
import matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.BeforeAndAfterEach

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Random
import org.scalatest.wordspec.AnyWordSpecLike

//todo: fix test names
class KillSelfRegisteredComponentTest
    extends ScalaTestWithActorTestKit
    with AnyWordSpecLike
    with MockitoSugar
    with BeforeAndAfterEach {

  private val locationService               = mock[LocationService]
  private val processExecutor               = mock[ProcessExecutor]
  private val process                       = mock[Process]
  private val logger                        = mock[Logger]
  private val agentSettings                 = AgentSettings("/tmp", 15.seconds, 3.seconds)
  implicit val scheduler: Scheduler         = system.scheduler
  private val prefix                        = Prefix("csw.component")
  private val componentId: ComponentId      = ComponentId(prefix, SequenceComponent)
  private val seqCompConn                   = AkkaConnection(componentId)
  private val seqCompLocation: AkkaLocation = AkkaLocation(seqCompConn, new URI("some"))
  private val seqCompLocationF              = Future.successful(Some(seqCompLocation))

  "Kill (self registered) Component" must {

    "reply 'killedGracefully' after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val spawner       = TestProbe[SpawnResponse]()
      val killer        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawner.ref, prefix)
      //wait it it is registered
      spawner.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(killer.ref, componentId)
      //ensure it is stopped
      killer.expectMessage(10.seconds, killedGracefully)
    }

    "reply 'killedForcefully' after stopping a registered component forcefully when it does not gracefully in given time | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForGracefulProcessTermination = 2.second))
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(5.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      //wait it it is registered
      probe1.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped
      probe2.expectMessage(killedForcefully)
    }

    "reply 'killedGracefully' after killing a running component when component is waiting registration confirmation | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 1.hour)) //this will actor remains in waiting state

      mockSuccessfulProcess(dieAfter = 3.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      //it should not be registered
      probe1.expectNoMessage(2.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)
    }

    "reply 'killedForcefully' after killing a running component when component is waiting registration confirmation | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForGracefulProcessTermination = 2.seconds))
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 1.hour)) //this will actor remains in waiting state

      mockSuccessfulProcess(dieAfter = 20.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      //it should not be registered
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedForcefully)
    }

    "reply 'killedGracefully' and cancel spawning of an already scheduled component when registration is being checked | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 1.hour)) //this will actor remains in checking state

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      //it should not be registered
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, componentId)
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)
    }

    "reply 'killedGracefully' after process termination, when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForGracefulProcessTermination = 7.seconds))
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawnProbe.ref, prefix)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, componentId)
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, componentId)

      //ensure it is stopped gracefully
      firstKiller.expectMessage(6.seconds, killedGracefully)
      secondKiller.expectMessage(Failed("process is already stopping"))
    }

    "reply 'Failed' when given component is not running on agent | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[KillResponse]()

      //try to stop the component
      agentActorRef ! KillComponent(probe.ref, ComponentId(Prefix("ESW.invalid"), SequenceComponent))

      //verify that response is Failure
      probe.expectMessage(Failed("given component id is not running on this agent"))
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

  private def spawnAgentActor(agentSettings: AgentSettings = agentSettings) = {
    spawn(new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty))
  }

  private def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    testKit.system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }
}
