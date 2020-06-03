package esw.agent.app

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
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

//todo: fix test names
class SpawnSelfRegisteredComponentTest extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "component-system")
  private val locationService                                     = mock[LocationService]
  private val processExecutor                                     = mock[ProcessExecutor]
  private val process                                             = mock[Process]
  private val processHandle                                       = mock[ProcessHandle]
  private val logger                                              = mock[Logger]

  private val agentSettings         = AgentSettings(15.seconds, Cs.channel)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix                        = Prefix("csw.component")
  private val componentId: ComponentId      = ComponentId(prefix, SequenceComponent)
  private val seqCompConn                   = AkkaConnection(componentId)
  private val seqCompLocation: AkkaLocation = AkkaLocation(seqCompConn, new URI("some"))
  private val seqCompLocationF              = Future.successful(Some(seqCompLocation))

  "SpawnSelfRegistered" must {
    "reply 'Spawned' and spawn component process | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor1")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Spawned)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor(name = "test-actor2")
      val probe         = TestProbe[SpawnResponse]()
      val err           = "Failed to resolve componet"
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.failed(new RuntimeException(err)))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed(err))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor3")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("can not spawn component when it is already registered in location service"))
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor4")
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      agentActorRef ! SpawnSequenceComponent(probe2.ref, prefix)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed("given component is already in process"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor5")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor(name = "test-actor6")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("registration encountered an issue or timed out"))
    }

    "reply 'Failed' when the process is spawned but exits before registration | ESW-237" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForComponentRegistration = 3.seconds), "test-actor7")
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(None, 15.seconds))

      mockSuccessfulProcess(dieAfter = 2.seconds)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(10.seconds, Failed("process died before registration"))
    }

    "reply 'Failed' when spawning is aborted by another message | ESW-237, ESW-276" in {
      val agentActorRef = spawnAgentActor(
        agentSettings.copy(durationToWaitForComponentRegistration = 7.seconds),
        "test-actor8"
      )
      val spawner = TestProbe[SpawnResponse]()
      val killer  = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 5.seconds))
      mockSuccessfulProcess(dieAfter = 2.seconds)

      agentActorRef ! SpawnSequenceComponent(spawner.ref, prefix)
      Thread.sleep(800)
      agentActorRef ! KillComponent(killer.ref, componentId)
      spawner.expectMessage(Failed("Aborted"))
      killer.expectMessage(Killed)
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, process, logger)
  }

  private def mockSuccessfulProcess(dieAfter: FiniteDuration = 2.seconds, exitCode: Int = 0) = {
    when(process.pid()).thenReturn(Random.nextInt(1000).abs)
    when(process.toHandle).thenReturn(processHandle)
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
}
