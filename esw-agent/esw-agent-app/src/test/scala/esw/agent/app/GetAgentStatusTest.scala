package esw.agent.app

import akka.actor.testkit.typed.scaladsl.TestProbe
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models._
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Prefix
import esw.agent.api.AgentCommand.GetAgentStatus
import esw.agent.api.AgentCommand.SpawnCommand.SpawnSelfRegistered.SpawnSequenceComponent
import esw.agent.api.ComponentStatus.Initializing
import esw.agent.api._
import esw.agent.app.AgentActor.AgentState
import esw.agent.app.process.ProcessExecutor
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.must.Matchers.convertToStringMustWrapper
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Future, Promise}

class GetAgentStatusTest extends AnyWordSpecLike with MockitoSugar with BeforeAndAfterEach {

  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "location-service-system")
  private val locationService                                     = mock[LocationService]
  private val processExecutor                                     = mock[ProcessExecutor]
  private val process                                             = mock[Process]
  private val logger                                              = mock[Logger]

  private val agentSettings         = AgentSettings("/tmp", 15.seconds, 3.seconds, Cs.channel)
  implicit val scheduler: Scheduler = system.scheduler

  "GetAgentStatus" must {

    "reply with a collection of status of all components available on the agent | ESW-286" in {
      val prefix1      = Prefix("csw.component1")
      val componentId1 = ComponentId(prefix1, SequenceComponent)

      val prefix2      = Prefix("csw.component2")
      val componentId2 = ComponentId(prefix2, SequenceComponent)

      val agentActorRef = spawnAgentActor()
      val spawner       = TestProbe[SpawnResponse]()
      val probe         = TestProbe[AgentStatus]()

      when(locationService.resolve(any[TypedConnection[AkkaLocation]], any[FiniteDuration]))
        .thenReturn(delayedFuture(None, 2.seconds))

      //spawn two processes
      agentActorRef ! SpawnSequenceComponent(spawner.ref, prefix1)
      agentActorRef ! SpawnSequenceComponent(spawner.ref, prefix2)

      //get agent status
      agentActorRef ! GetAgentStatus(probe.ref)

      //ensure both components are initializing
      probe.expectMessage(AgentStatus(Map(componentId1 -> Initializing, componentId2 -> Initializing)))
    }
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(locationService, processExecutor, process, logger)
  }

  private def spawnAgentActor(agentSettings: AgentSettings = agentSettings) = {
    system.systemActorOf(
      new AgentActor(locationService, processExecutor, agentSettings, logger).behavior(AgentState.empty),
      "test-actor"
    )
  }

  private def delayedFuture[T](value: T, delay: FiniteDuration): Future[T] = {
    val promise = Promise[T]()
    system.scheduler.scheduleOnce(delay, () => promise.success(value))(system.executionContext)
    promise.future
  }
}
