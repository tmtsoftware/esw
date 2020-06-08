package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.api.{Failed, SpawnResponse, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.commons.{BaseTestSuite, Timeouts}
import esw.ocs.api.SequenceComponentApi
import esw.sm.api.models.CommonFailure.LocationServiceError
import esw.sm.api.models.SequenceManagerError.SpawnSequenceComponentFailed
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

      mockSpawnComponent(Spawned)
      when(locationServiceUtil.resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout)))
        .thenReturn(futureRight(sequenceComponentLocation))

      agentUtil.spawnSequenceComponentFor(ESW).rightValue shouldBe a[SequenceComponentApi]

      verifySpawnSequenceComponentCalled()
      verify(locationServiceUtil).resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout))
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      val spawnFailed = Failed("failed to spawn sequence component")
      mockSpawnComponent(spawnFailed)

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(SpawnSequenceComponentFailed(spawnFailed.msg))

      verifySpawnSequenceComponentCalled()
    }

    "return LocationServiceError if location service call to resolve spawned sequence returns error | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      mockSpawnComponent(Spawned)
      when(locationServiceUtil.resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout)))
        .thenReturn(futureLeft(LocationNotFound("Could not resolve sequence component")))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(LocationServiceError("Could not resolve sequence component"))

      verifySpawnSequenceComponentCalled()
      verify(locationServiceUtil).resolve(any[AkkaConnection], argEq(Timeouts.DefaultTimeout))
    }

    "return LocationServiceError if getAgent fails | ESW-164" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override private[sm] def getAgent: Future[Either[EswLocationError, AgentClient]] =
          futureLeft(LocationNotFound("Error in agent"))
      }

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(LocationServiceError("Error in agent"))
    }
  }

  "getAgent" must {
    "return AgentClient associated to ESW machine | ESW-164" in {
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

    "return LocationNotFound when location service list call returns empty list | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureRight(List.empty))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getAgent.leftValue shouldBe a[LocationNotFound]

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }

    "return LocationNotFound when location service list call returns error | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val listingFailed       = RegistrationListingFailed("listing failed")
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureLeft(listingFailed))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getAgent.leftValue should ===(listingFailed)

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }
  }

  "makeAgent" must {
    "create new instance of AgentClient | ESW-164" in {
      val agentPrefix         = Prefix(ESW, "mock")
      val locationServiceUtil = mock[LocationServiceUtil]
      val locationService     = mock[LocationService]
      val agentConnection     = AkkaConnection(ComponentId(agentPrefix, Machine))
      val location            = AkkaLocation(agentConnection, new URI("mock"))
      val mockedAgentLoc      = Future.successful(Some(location))

      when(locationServiceUtil.locationService).thenReturn(locationService)
      when(locationService.resolve(agentConnection, 5.seconds)).thenReturn(mockedAgentLoc)

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.makeAgent(agentPrefix).futureValue shouldBe a[AgentClient]
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

    def mockSpawnComponent(response: SpawnResponse): Unit =
      when(agentClient.spawnSequenceComponent(any[Prefix], any[Option[String]]))
        .thenReturn(Future.successful(response))

    def verifySpawnSequenceComponentCalled(): Unit =
      verify(agentClient).spawnSequenceComponent(any[Prefix], any[Option[String]])
  }
}
