package esw.sm.impl.utils

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.BaseTestSuite
import esw.commons.utils.location.ComponentFactory
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.models.SequenceManagerError.{LocationServiceError, SpawnSequenceComponentFailed}
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class AgentUtilTest extends BaseTestSuite {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")
  implicit val timeout: Timeout                                = 1.hour

  override def afterAll(): Unit = {
    super.afterAll()
    actorSystem.terminate()
  }

  "spawnSequenceComponentFor" must {
    "return SequenceComponentApi after spawning sequence component | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix])).thenReturn(Future.successful(Spawned))
      when(componentFactory.resolveSeqComp(any[Prefix]))
        .thenReturn(Future.successful(Right(sequenceComponentImpl)))
      when(componentFactory.findAgent(ESW))
        .thenReturn(Future.successful(Right(agentClient)))

      agentUtil.spawnSequenceComponentFor(ESW).rightValue shouldBe a[SequenceComponentApi]

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(componentFactory).resolveSeqComp(any[Prefix])
      verify(componentFactory).findAgent(ESW)
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix]))
        .thenReturn(Future.successful(Failed("failed to spawn sequence component")))
      when(componentFactory.findAgent(ESW))
        .thenReturn(Future.successful(Right(agentClient)))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe SpawnSequenceComponentFailed(
        "failed to spawn sequence component"
      )

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(componentFactory).findAgent(ESW)
    }

    "return LocationServiceError if location service call to resolve spawned sequence returns error | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix]))
        .thenReturn(Future.successful(Spawned))
      when(componentFactory.resolveSeqComp(any[Prefix]))
        .thenReturn(Future.successful(Left(ResolveLocationFailed("Could not resolve sequence component"))))
      when(componentFactory.findAgent(ESW))
        .thenReturn(Future.successful(Right(agentClient)))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe LocationServiceError("Could not resolve sequence component")

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(componentFactory).resolveSeqComp(any[Prefix])
      verify(componentFactory).findAgent(ESW)
    }

    "return error if find agent fails | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      when(componentFactory.findAgent(ESW))
        .thenReturn(Future.successful(Left(ResolveLocationFailed("could not find agent for subsystem"))))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe LocationServiceError("could not find agent for subsystem")

      verify(componentFactory).findAgent(ESW)
    }
  }

  class TestSetup() {
    val componentFactory: ComponentFactory           = mock[ComponentFactory]
    val sequenceComponentImpl: SequenceComponentImpl = mock[SequenceComponentImpl]
    val agentClient: AgentClient                     = mock[AgentClient]

    val agentUtil: AgentUtil = new AgentUtil(componentFactory)
  }
}
