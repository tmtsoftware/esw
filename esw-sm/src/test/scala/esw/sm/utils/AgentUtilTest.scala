package esw.sm.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.BaseTestSuite
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import org.mockito.ArgumentMatchers.{any, eq => argEq}

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
      when(locationServiceUtil.resolveAkkaLocation(any[Prefix], argEq(SequenceComponent)))
        .thenReturn(Future.successful(sequenceComponentLocation))

      agentUtil.spawnSequenceComponentFor(ESW).rightValue shouldBe a[SequenceComponentApi]

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(locationServiceUtil).resolveAkkaLocation(any[Prefix], argEq(SequenceComponent))
    }

    "return SequencerError if agent fails to spawn sequence component | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix]))
        .thenReturn(Future.successful(Failed("failed to spawn sequence component")))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe SequencerError("failed to spawn sequence component")

      verify(agentClient).spawnSequenceComponent(any[Prefix])
    }

    "return SequencerError if location service call to resolve spawned sequence component throws exception | ESW-178" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix]))
        .thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolveAkkaLocation(any[Prefix], argEq(SequenceComponent)))
        .thenReturn(Future.failed(new RuntimeException("Exception: could not resolve sequence component")))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe SequencerError(
        "Exception: could not resolve sequence component"
      )

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(locationServiceUtil).resolveAkkaLocation(any[Prefix], argEq(SequenceComponent))
    }

    "return SequencerError if getAgent fails | ESW-178" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override private[sm] def getAgent: Future[AgentClient] = Future.failed(new RuntimeException("Error in agent"))
      }

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe SequencerError("Error in agent")
    }
  }

  class TestSetup() {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

    val agentClient: AgentClient = mock[AgentClient]

    val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
      override private[sm] def getAgent: Future[AgentClient] = Future.successful(agentClient)
    }

    val sequenceComponentLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), SequenceComponent)), new URI("some-uri"))

  }
}
