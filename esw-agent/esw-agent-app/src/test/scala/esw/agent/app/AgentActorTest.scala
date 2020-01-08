package esw.agent.app

import java.net.URI

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
import esw.agent.api.Response.{Failed, Spawned}
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.utils.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.MustMatchers.convertToStringMustWrapper
import org.scalatest.{BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}

class AgentActorTest extends ScalaTestWithActorTestKit with WordSpecLike with MockitoSugar with BeforeAndAfterEach {

  private val locationService       = mock[LocationService]
  private val processExecutor       = mock[ProcessExecutor]
  private val logger                = mock[Logger]
  private val agentSettings         = AgentSettings("/tmp", 15.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix                        = Prefix("tcs.tcs_darknight")
  private val seqCompConn                   = AkkaConnection(ComponentId(prefix, SequenceComponent))
  private val seqCompLocation: AkkaLocation = AkkaLocation(seqCompConn, new URI("some"))
  private val seqCompLoc                    = Future.successful(Some(seqCompLocation))

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, logger)
  }

  // common mocks
  when(processExecutor.killProcess(any[Long])).thenReturn(true)

  "SpawnSequenceComponent" must {

    "reply 'Spawned' and spawn a new sequence component process | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Spawned)
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

    "reply 'Failed' and not spawn new process when it is already being spawned by a previous message | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[Response]()
      val probe2        = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 200.millis))

      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      agentActorRef ! SpawnSequenceComponent(probe2.ref, prefix)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed("spawning of component is already in progress"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLoc)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left(Failed("failure")))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' when the process is spawned but failed to register itself | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[Response]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Right(1234))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("could not get registration confirmation from spawned process within given time"))
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
