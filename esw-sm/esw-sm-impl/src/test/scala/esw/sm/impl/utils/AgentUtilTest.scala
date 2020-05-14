package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.{Failed, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
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
        .thenReturn(futureRight(sequenceComponentLocation))

      agentUtil.spawnSequenceComponentFor(ESW).rightValue shouldBe a[SequenceComponentApi]

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(locationServiceUtil).resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout))
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      val spawnFailed = Failed("failed to spawn sequence component")
      when(agentClient.spawnSequenceComponent(any[Prefix])).thenReturn(Future.successful(spawnFailed))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(SpawnSequenceComponentFailed(spawnFailed.msg))

      verify(agentClient).spawnSequenceComponent(any[Prefix])
    }

    "return LocationServiceError if location service call to resolve spawned sequence returns error | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      when(agentClient.spawnSequenceComponent(any[Prefix])).thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout)))
        .thenReturn(futureLeft(ResolveLocationFailed("Could not resolve sequence component")))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(LocationServiceError("Could not resolve sequence component"))

      verify(agentClient).spawnSequenceComponent(any[Prefix])
      verify(locationServiceUtil).resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout))
    }

    "return LocationServiceError if getAgent fails | ESW-164" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override private[sm] def getAgent: Future[Either[EswLocationError, AgentClient]] =
          futureLeft(ResolveLocationFailed("Error in agent"))
      }

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(LocationServiceError("Error in agent"))
    }
  }

  "getAgent" must {
    "must return AgentClient associated to ESW machine | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentClient         = mock[AgentClient]
      val location            = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "mock"), Machine)), new URI("mock"))

      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureRight(List(location)))

      val agentUtil = new AgentUtil(locationServiceUtil) {
        override private[utils] def makeAgent(prefix: Prefix) = Future.successful(agentClient)
      }

      agentUtil.getAgent.rightValue should ===(agentClient)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }

    "must return ResolveLocationFailed when location service list call returns empty list | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureRight(List.empty))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getAgent.leftValue shouldBe a[ResolveLocationFailed]

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }

    "must return ResolveLocationFailed when location service list call returns error | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val listingFailed       = RegistrationListingFailed("listing failed")
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureLeft(listingFailed))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getAgent.leftValue should ===(listingFailed)

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
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
