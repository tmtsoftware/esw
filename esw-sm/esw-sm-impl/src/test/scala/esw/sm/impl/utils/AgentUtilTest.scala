package esw.sm.impl.utils

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
import esw.commons.utils.location.EswLocationError.ResolveLocationFailed
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.SequenceComponentApi
import esw.sm.api.models.SequenceManagerError.{LocationServiceError, SpawnSequenceComponentFailed}
import org.mockito.ArgumentMatchers.{any, eq => argEq}

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class AgentUtilTest extends BaseTestSuite {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")
  implicit val timeout: Timeout                                = 1.hour

  override def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }

  "spawnSequenceComponentFor" must {
    "return SequenceComponentApi after spawning sequence component | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix])).thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout)))
        .thenReturn(Future.successful(Right(sequenceComponentLocation)))

      agentUtil.spawnSequenceComponentFor(ESW).rightValue shouldBe a[SequenceComponentApi]

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(locationServiceUtil).resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout))
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix]))
        .thenReturn(Future.successful(Failed("failed to spawn sequence component")))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe
      SpawnSequenceComponentFailed("failed to spawn sequence component")

      verify(agentClient).spawnSequenceComponent(any[Prefix])
    }

    "return LocationServiceError if location service call to resolve spawned sequence returns error | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix])).thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout)))
        .thenReturn(Future.successful(Left(ResolveLocationFailed("Could not resolve sequence component"))))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe LocationServiceError("Could not resolve sequence component")

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(locationServiceUtil).resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout))
    }

    "return LocationServiceError if getAgent fails | ESW-164" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override private[sm] def getAgent: Future[Either[EswLocationError, AgentClient]] =
          Future.successful(Left(ResolveLocationFailed("Error in agent")))
      }

      agentUtil.spawnSequenceComponentFor(ESW).leftValue shouldBe LocationServiceError("Error in agent")
    }
  }

  class TestSetup() {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
    val agentClient: AgentClient                 = mock[AgentClient]

    val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
      override private[sm] def getAgent: Future[Either[EswLocationError, AgentClient]] = Future.successful(Right(agentClient))
    }

    val sequenceComponentLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), SequenceComponent)), new URI("some-uri"))

  }
}
