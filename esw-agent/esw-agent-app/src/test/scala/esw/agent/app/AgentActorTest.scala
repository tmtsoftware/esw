package esw.agent.app

import java.net.URI
import java.util.concurrent.CompletableFuture

import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import akka.actor.typed.Scheduler
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ComponentType.{SequenceComponent, Service}
import csw.location.models.Connection.{AkkaConnection, TcpConnection}
import csw.location.models.{AkkaLocation, ComponentId, TcpLocation, TcpRegistration}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.KillComponent
import esw.agent.api.AgentCommand.SpawnManuallyRegistered.SpawnRedis
import esw.agent.api.AgentCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.Killed._
import esw.agent.api._
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.ProcessExecutor
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.MockitoSugar
import org.scalatest.MustMatchers.convertToStringMustWrapper
import org.scalatest.{BeforeAndAfterEach, WordSpecLike}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}
import scala.util.Random

class AgentActorTest extends ScalaTestWithActorTestKit with WordSpecLike with MockitoSugar with BeforeAndAfterEach {

  private val locationService = mock[LocationService]
  private val processExecutor = mock[ProcessExecutor]
  private val process         = mock[Process]
  private val logger          = mock[Logger]

  private val agentSettings         = AgentSettings("/tmp", 15.seconds, 3.seconds)
  implicit val scheduler: Scheduler = system.scheduler

  private val prefix = Prefix("csw.component")

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

  "SpawnSelfRegistered" must {

    val seqCompConn                   = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val seqCompLocation: AkkaLocation = AkkaLocation(seqCompConn, new URI("some"))
    val seqCompLocationF              = Future.successful(Some(seqCompLocation))

    "reply 'Spawned' and spawn component process | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Spawned)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.failed(new RuntimeException("call failed")))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("error occurred while resolving a component with location service"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(seqCompLocationF)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("can not spawn component when it is already registered in location service"))
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor()
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
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)
      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))

      mockSuccessfulProcess()

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(Failed("could not get registration confirmation from spawned process"))
    }

    "reply 'Failed' when the process is spawned but exits before registration | ESW-237" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForComponentRegistration = 3.seconds))
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(None, 15.seconds))

      mockSuccessfulProcess(dieAfter = 2.seconds)

      agentActorRef ! SpawnSequenceComponent(probe.ref, prefix)
      probe.expectMessage(10.seconds, Failed("process died before registration confirmation"))
    }

    "reply 'Failed' when spawning is aborted by another message | ESW-237, ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 1.minute))
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, SequenceComponent))
      probe1.expectMessage(10.seconds, Failed("Aborted"))
      probe2.expectMessage(killedGracefully)
    }
  }

  "SpawnManuallyRegistered" must {

    val redisConn               = TcpConnection(ComponentId(prefix, Service))
    val redisLocation           = TcpLocation(redisConn, new URI("some"))
    val redisLocationF          = Future.successful(Some(redisLocation))
    val redisRegistration       = TcpRegistration(redisConn, 100)
    val redisRegistrationResult = RegistrationResult.from(redisLocation, con => locationService.unregister(con))

    def mockLocationServiceForRedis(registrationDuration: FiniteDuration = 0.seconds) = {
      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))
      when(locationService.register(redisRegistration))
        .thenReturn(delayedFuture(redisRegistrationResult, registrationDuration))
    }

    val spawnRedis = SpawnRedis(_, prefix, 100, List.empty)

    "reply 'Spawned' and spawn component process | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Spawned)
    }

    "reply 'Failed' and not spawn new process when call to location service fails" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.failed(new RuntimeException("call failed")))

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("error occurred while resolving a component with location service"))
    }

    "reply 'Failed' and not spawn new process when it is already registered with location service | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(redisLocationF)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("can not spawn component when it is already registered in location service"))
    }

    "reply 'Failed' and not spawn new process when it is already spawned on the agent | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe1.ref)
      agentActorRef ! spawnRedis(probe2.ref)

      probe1.expectMessage(Spawned)
      probe2.expectMessage(Failed("given component is already in process"))
    }

    "reply 'Failed' when process fails to spawn | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis()

      when(processExecutor.runCommand(any[List[String]], any[Prefix])).thenReturn(Left("failure"))

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("failure"))
    }

    "reply 'Failed' and kill process, when the process is spawned but failed to register | ESW-237" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[SpawnResponse]()

      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))

      when(locationService.register(redisRegistration))
        .thenReturn(Future.failed(new RuntimeException("failure")))

      mockSuccessfulProcess()

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(Failed("could not register spawned process"))
    }

    "reply 'Failed' when the process is spawned but exits before registration | ESW-237" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForComponentRegistration = 3.seconds))
      val probe         = TestProbe[SpawnResponse]()

      mockLocationServiceForRedis(registrationDuration = 15.seconds)
      mockSuccessfulProcess(dieAfter = 2.seconds)

      agentActorRef ! spawnRedis(probe.ref)
      probe.expectMessage(10.seconds, Failed("process died before registration completion"))
    }

    "reply 'Failed' when spawning is aborted by another message | ESW-237, ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.minute)
      mockSuccessfulProcess(10.seconds)

      agentActorRef ! spawnRedis(probe1.ref)
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, Service))
      probe1.expectMessage(3.seconds, Failed("Aborted"))
      probe2.expectMessage(killedGracefully)
    }
  }

  "KillComponent (self registered)" must {

    val seqCompConn                   = AkkaConnection(ComponentId(prefix, SequenceComponent))
    val seqCompLocation: AkkaLocation = AkkaLocation(seqCompConn, new URI("some"))
    val seqCompLocationF              = Future.successful(Some(seqCompLocation))

    "reply 'killedGracefully' after stopping a registered component gracefully | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 2.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      //wait it it is registered
      probe1.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, SequenceComponent))
      //ensure it is stopped
      probe2.expectMessage(10.seconds, killedGracefully)
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
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, SequenceComponent))
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
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, SequenceComponent))
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)
    }

    "reply 'killedForcefully' after killing a running component when component is waiting registration confirmation | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), delayedFuture(Some(seqCompLocation), 1.hour)) //this will actor remains in waiting state

      mockSuccessfulProcess(dieAfter = 20.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(probe1.ref, prefix)
      //it should not be registered
      probe1.expectNoMessage(5.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, SequenceComponent))
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
      probe1.expectNoMessage(5.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, SequenceComponent))
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

      mockSuccessfulProcess(dieAfter = 5.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawnProbe.ref, prefix)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, ComponentId(prefix, SequenceComponent))
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, ComponentId(prefix, SequenceComponent))

      //ensure it is stopped gracefully
      firstKiller.expectMessage(6.seconds, killedGracefully)
      secondKiller.expectMessage(killedGracefully)
    }

    "reply 'killedForcefully' after process termination, when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()
      when(locationService.resolve(argEq(seqCompConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None), seqCompLocationF)

      mockSuccessfulProcess(dieAfter = 5.seconds)

      //start a component
      agentActorRef ! SpawnSequenceComponent(spawnProbe.ref, prefix)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, ComponentId(prefix, SequenceComponent))
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, ComponentId(prefix, SequenceComponent))

      //ensure it is stopped forcefully
      firstKiller.expectMessage(4.seconds, killedForcefully)
      secondKiller.expectMessage(killedForcefully)
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

  "KillComponent (manually registered)" must {

    val redisConn               = TcpConnection(ComponentId(prefix, Service))
    val redisLocation           = TcpLocation(redisConn, new URI("some"))
    val redisRegistration       = TcpRegistration(redisConn, 100)
    val redisRegistrationResult = RegistrationResult.from(redisLocation, con => locationService.unregister(con))

    def mockLocationServiceForRedis(registrationDuration: FiniteDuration = 0.seconds) = {
      when(locationService.resolve(argEq(redisConn), any[FiniteDuration]))
        .thenReturn(Future.successful(None))
      when(locationService.register(redisRegistration))
        .thenReturn(delayedFuture(redisRegistrationResult, registrationDuration))
    }

    val spawnRedis = SpawnRedis(_, prefix, 100, List.empty)

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
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, Service))
      //ensure it is stopped
      probe2.expectMessage(10.seconds, killedGracefully)
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
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, Service))
      //ensure it is stopped
      probe2.expectMessage(killedForcefully)
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
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, Service))
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)
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
      probe1.expectNoMessage(5.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, Service))
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedForcefully)
    }

    "reply 'killedGracefully' and cancel spawning of an already scheduled component when registration is being performed | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe1        = TestProbe[SpawnResponse]()
      val probe2        = TestProbe[KillResponse]()

      mockLocationServiceForRedis(1.hour) //this will actor remains in registering state
      mockSuccessfulProcess(4.seconds)

      //start a component
      agentActorRef ! spawnRedis(probe1.ref)
      probe1.expectNoMessage(1.seconds)

      //stop the component
      agentActorRef ! KillComponent(probe2.ref, ComponentId(prefix, Service))
      //ensure it is stopped gracefully
      probe2.expectMessage(10.seconds, killedGracefully)
    }

    "reply 'killedGracefully' after process termination, when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor(agentSettings.copy(durationToWaitForGracefulProcessTermination = 7.seconds))
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(dieAfter = 5.seconds)

      //start a component
      agentActorRef ! spawnRedis(spawnProbe.ref)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, ComponentId(prefix, Service))
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, ComponentId(prefix, Service))

      //ensure it is stopped gracefully
      firstKiller.expectMessage(6.seconds, killedGracefully)
      secondKiller.expectMessage(killedGracefully)
    }

    "reply 'killedForcefully' after process termination, when process is already stopping by another message | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val spawnProbe    = TestProbe[SpawnResponse]()
      val firstKiller   = TestProbe[KillResponse]()
      val secondKiller  = TestProbe[KillResponse]()

      mockLocationServiceForRedis()
      mockSuccessfulProcess(dieAfter = 5.seconds)

      //start a component
      agentActorRef ! spawnRedis(spawnProbe.ref)
      spawnProbe.expectMessage(Spawned)

      //stop the component
      agentActorRef ! KillComponent(firstKiller.ref, ComponentId(prefix, Service))
      //stop the component again
      agentActorRef ! KillComponent(secondKiller.ref, ComponentId(prefix, Service))

      //ensure it is stopped forcefully
      firstKiller.expectMessage(4.seconds, killedForcefully)
      secondKiller.expectMessage(killedForcefully)
    }

    "reply 'Failed' when given component is not running on agent | ESW-276" in {
      val agentActorRef = spawnAgentActor()
      val probe         = TestProbe[KillResponse]()

      //try to stop the component
      agentActorRef ! KillComponent(probe.ref, ComponentId(Prefix("ESW.invalid"), Service))

      //verify that response is Failure
      probe.expectMessage(Failed("given component id is not running on this agent"))
    }
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
