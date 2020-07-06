package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import csw.prefix.models.{Prefix, Subsystem}
import esw.agent.api.{Failed, SpawnResponse, Spawned}
import esw.agent.client.AgentClient
import esw.commons.Timeouts
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.sm.api.protocol.AgentError
import esw.sm.api.protocol.AgentError.SpawnSequenceComponentFailed
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.testcommons.BaseTestSuite
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import org.mockito.captor.ArgCaptor

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
      val connectionCaptor = ArgCaptor[AkkaConnection]
      val prefixCaptor     = ArgCaptor[Prefix]

      when(agentClient.spawnSequenceComponent(prefixCaptor.capture, argEq(None))).thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolve(connectionCaptor.capture, argEq(Timeouts.DefaultResolveLocationDuration)))
        .thenReturn(futureRight(sequenceComponentLocation))

      agentUtil.spawnSequenceComponentFor(ESW).rightValue shouldBe a[SequenceComponentApi]

      connectionCaptor.value.prefix.subsystem shouldBe ESW
      prefixCaptor.value.subsystem shouldBe ESW

      // verify random name generation logic
      val name                          = prefixCaptor.value.componentName
      val (subsystemName, randomNumber) = name.splitAt(name.indexOf('_'))
      subsystemName shouldBe ESW.entryName
      (1 to 100) should contain(randomNumber.drop(1).toLong)
    }

    "return SequenceComponentApi after spawning sequence component for given name and agent prefix | ESW-337" in {
      val setup = new TestSetup()
      import setup._

      val seqCompName       = "seq_comp"
      val agentPrefix       = Prefix(IRIS, "primary")
      val seqCompConnection = AkkaConnection(ComponentId(Prefix(IRIS, seqCompName), SequenceComponent))

      val connectionCaptor = ArgCaptor[AkkaConnection]
      val prefixCaptor     = ArgCaptor[Prefix]

      when(agentClient.spawnSequenceComponent(prefixCaptor.capture, argEq(None))).thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolve(connectionCaptor.capture, argEq(Timeouts.DefaultResolveLocationDuration)))
        .thenReturn(futureRight(sequenceComponentLocation))

      agentUtil.spawnSequenceComponentFor(agentPrefix, seqCompName).rightValue shouldBe a[SequenceComponentApi]

      connectionCaptor.value shouldBe seqCompConnection
      prefixCaptor.value shouldBe seqCompConnection.prefix
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      val spawnFailed = Failed("failed to spawn sequence component")

      val prefixCaptor = ArgCaptor[Prefix]
      when(agentClient.spawnSequenceComponent(prefixCaptor.capture, argEq(None))).thenReturn(Future.successful(spawnFailed))

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(SpawnSequenceComponentFailed(spawnFailed.msg))

      prefixCaptor.value.subsystem shouldBe ESW
    }

    "return LocationServiceError if location service call to resolve spawned sequence returns error | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      val connectionCaptor = ArgCaptor[AkkaConnection]
      val prefixCaptor     = ArgCaptor[Prefix]

      when(agentClient.spawnSequenceComponent(prefixCaptor.capture, argEq(None))).thenReturn(Future.successful(Spawned))
      when(locationServiceUtil.resolve(connectionCaptor.capture, argEq(Timeouts.DefaultResolveLocationDuration)))
        .thenReturn(futureLeft(LocationNotFound("Could not resolve sequence component")))

      agentUtil.spawnSequenceComponentFor(TCS).leftValue should ===(LocationServiceError("Could not resolve sequence component"))

      connectionCaptor.value.componentId.prefix.subsystem shouldBe TCS
      prefixCaptor.value.subsystem shouldBe TCS
    }

    "return LocationServiceError if getAgent fails | ESW-164, ESW-337" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

      val errorMsg = "Error in agent"
      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override private[sm] def getRandomAgent(subsystem: Subsystem): Future[Either[AgentError, AgentClient]] =
          futureLeft(LocationServiceError(errorMsg))

        override private[sm] def getAgent(prefix: Prefix): Future[Either[AgentError, AgentClient]] =
          futureLeft(LocationServiceError(errorMsg))
      }

      agentUtil.spawnSequenceComponentFor(ESW).leftValue should ===(LocationServiceError(errorMsg))
      agentUtil.spawnSequenceComponentFor(Prefix(ESW, "invalid"), "invalid").leftValue should ===(LocationServiceError(errorMsg))
    }
  }

  "getRandomAgent" must {
    "return AgentClient associated to ESW machine | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentClient         = mock[AgentClient]
      val location            = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "mock"), Machine)), new URI("mock"))

      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureRight(List(location)))

      val agentUtil = new AgentUtil(locationServiceUtil) {
        override private[utils] def makeAgent(prefix: Prefix) = Future.successful(agentClient)
      }

      agentUtil.getRandomAgent(ESW).rightValue should ===(agentClient)
      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }

    "return LocationNotFound when location service list call returns empty list | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureRight(List.empty))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getRandomAgent(ESW).leftValue shouldBe a[LocationServiceError]

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }

    "return LocationNotFound when location service list call returns error | ESW-164" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val listingFailed       = RegistrationListingFailed("listing failed")
      when(locationServiceUtil.listAkkaLocationsBy(ESW, Machine)).thenReturn(futureLeft(listingFailed))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getRandomAgent(ESW).leftValue shouldBe a[LocationServiceError]

      verify(locationServiceUtil).listAkkaLocationsBy(ESW, Machine)
    }
  }

  "getAgent" must {
    "return AgentClient associated to ESW machine | ESW-337" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentClient         = mock[AgentClient]
      val agentPrefix         = Prefix(ESW, "primary")
      val connection          = AkkaConnection(ComponentId(agentPrefix, Machine))
      val location            = AkkaLocation(connection, new URI("mock"))

      when(locationServiceUtil.find(connection)).thenReturn(futureRight(location))

      val agentUtil = new AgentUtil(locationServiceUtil) {
        override private[utils] def makeAgent(prefix: Prefix) = Future.successful(agentClient)
      }

      agentUtil.getAgent(agentPrefix).rightValue should ===(agentClient)
      verify(locationServiceUtil).find(connection)
    }

    "return LocationNotFound when location service find call returns LocationNotFound | ESW-337" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentPrefix         = Prefix(ESW, "primary")
      val connection          = AkkaConnection(ComponentId(agentPrefix, Machine))
      val locationNotFound    = LocationNotFound("location not found")

      when(locationServiceUtil.find(connection)).thenReturn(futureLeft(locationNotFound))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getAgent(agentPrefix).leftValue should ===(LocationServiceError(locationNotFound.msg))

      verify(locationServiceUtil).find(connection)
    }

    "return RegistrationListingFailed when location service find call returns RegistrationListingFailed | ESW-337" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentPrefix         = Prefix(ESW, "primary")
      val connection          = AkkaConnection(ComponentId(agentPrefix, Machine))
      val listingFailed       = RegistrationListingFailed("listing failed")

      when(locationServiceUtil.find(connection)).thenReturn(futureLeft(listingFailed))

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.getAgent(agentPrefix).leftValue should ===(LocationServiceError(listingFailed.msg))

      verify(locationServiceUtil).find(connection)
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
      when(locationService.find(agentConnection)).thenReturn(mockedAgentLoc)

      val agentUtil = new AgentUtil(locationServiceUtil)
      agentUtil.makeAgent(agentPrefix).futureValue shouldBe a[AgentClient]
    }
  }

  class TestSetup() {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
    val agentClient: AgentClient                 = mock[AgentClient]

    val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
      override private[sm] def getRandomAgent(subsystem: Subsystem): Future[Either[AgentError, AgentClient]] =
        Future.successful(Right(agentClient))

      override private[sm] def getAgent(prefix: Prefix): Future[Either[AgentError, AgentClient]] =
        Future.successful(Right(agentClient))
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
