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
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSequenceComponent
import esw.agent.api.Response
import esw.agent.api.Response.{Failed, Ok}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.utils.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.MustMatchers.convertToStringMustWrapper
import org.scalatest.{BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}

class AgentActorTest extends ScalaTestWithActorTestKit with WordSpecLike with MockitoSugar with BeforeAndAfterEach {

  private val locationService = mock[LocationService]
  private val processExecutor = mock[ProcessExecutor]
  private val processHandle   = mock[ProcessHandle]
  private val logger          = mock[Logger]

  private val agentSettings         = AgentSettings("/tmp", 15.seconds, 2.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix                        = Prefix("tcs.tcs_darknight")
  private val seqCompConn                   = AkkaConnection(ComponentId(prefix, SequenceComponent))
  private val seqCompLocation: AkkaLocation = AkkaLocation(seqCompConn, new URI("some"))
  private val seqCompLoc                    = Future.successful(Some(seqCompLocation))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, processHandle, logger)
  }

  private def mockSuccessfulProcessHandle(dieAfter: FiniteDuration = 5.seconds) = {
    when(processHandle.pid()).thenReturn(1234)
    val future = new CompletableFuture[ProcessHandle]()
    scheduler.scheduleOnce(dieAfter, () => future.complete(processHandle))
    when(processHandle.onExit()).thenReturn(future)
  }

  "SpawnSequenceComponent" must {

    "reply 'Ok' and spawn a new sequence component process | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)
      mockSuccessfulProcessHandle()
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(processHandle))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Ok)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.failed(new RuntimeException("call failed")))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("error occurred while resolving a component with location service"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(seqCompLoc)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("can not spawn component when it is already registered"))
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[Response]()
      val probe2        = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)

      mockSuccessfulProcessHandle()

      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(processHandle))

      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      agentActorRef ! SpawnSequenceComponent(probe2.ref, prefix)

      probe1.expectMessage(Ok)
      probe2.expectMessage(Failed("given component is already in process"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register itself | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))
      mockSuccessfulProcessHandle()
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(processHandle))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("could not get registration confirmation from spawned process within given time"))
    }

    "reply 'Failed' when the process is spawned but exits before registration | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 6.seconds))
      mockSuccessfulProcessHandle(1.seconds)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(processHandle))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(10.seconds, Failed("process died before registration confirmation"))
    }

    "reply 'Failed' when spawning is aborted by another message | ESW-237" in {
      ???
    }
  }

  "KillComponent" must {

    "reply 'Ok' and kill the running component when component gracefully is registered | ESW-237" in {
      ???
    }

    "reply 'Ok' and kill the running component forcefully if it does not gracefully in given time" in { ??? }

    "reply 'Ok' and kill the running component when component is waiting registration confirmation | ESW-237" in {
      ???
    }

    "reply 'Ok' and cancel spawning of an already scheduled component when registration is being checked | ESW-237" in {
      ???
    }

    "reply 'Ok' and when process is already stopping" in { ??? }

    "reply 'Failed' when given component is not running on agent | ESW-237" in {
      ???
    }
  }

  private def spawnAgentActor() = {
    spawn(new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty))
  }

  private def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    testKit.system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }
}
