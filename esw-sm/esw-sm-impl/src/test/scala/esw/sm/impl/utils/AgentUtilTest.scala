package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{CSW, ESW, IRIS, TCS}
import esw.agent.api.ComponentStatus.{Initializing, Running}
import esw.agent.api.{AgentStatus, Failed, SpawnResponse, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.SpawnSequenceComponentResponse.SpawnSequenceComponentFailed
import esw.sm.api.protocol.{ProvisionResponse, SpawnSequenceComponentResponse}
import esw.sm.impl.config.ProvisionConfig
import esw.testcommons.BaseTestSuite
import org.mockito.ArgumentMatchers.any

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
    "return Success after spawning sequence component for given name and machine prefix | ESW-337" in {
      val setup = new TestSetup()
      import setup._

      val seqCompName       = "seq_comp"
      val seqCompPrefix     = Prefix(IRIS, seqCompName)
      val seqCompConnection = AkkaConnection(ComponentId(seqCompPrefix, SequenceComponent))
      val machinePrefix     = Prefix(IRIS, "primary")

      when(agentClient.spawnSequenceComponent(seqCompPrefix, None)).thenReturn(Future.successful(Spawned))

      agentUtil.spawnSequenceComponent(machinePrefix, seqCompName).futureValue shouldBe SpawnSequenceComponentResponse.Success(
        seqCompConnection.componentId
      )

      verify(agentClient).spawnSequenceComponent(seqCompPrefix, None)
    }

    "return SpawnSequenceComponentFailed if agent fails to spawn sequence component | ESW-164" in {
      val setup = new TestSetup()
      import setup._

      val componentName = "testComp"
      val prefix        = Prefix(TCS, componentName)
      val spawnFailed   = Failed("failed to spawn sequence component")

      when(agentClient.spawnSequenceComponent(prefix, None)).thenReturn(Future.successful(spawnFailed))

      agentUtil.spawnSequenceComponent(prefix, componentName).futureValue should ===(
        SpawnSequenceComponentFailed(spawnFailed.msg)
      )
    }

    "return LocationServiceError if getAgent fails | ESW-164, ESW-337" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]

      val errorMsg = "Error in agent"
      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override private[sm] def getAgent(prefix: Prefix): Future[Either[LocationServiceError, AgentClient]] =
          futureLeft(LocationServiceError(errorMsg))
      }

      agentUtil.spawnSequenceComponent(Prefix(ESW, "invalid"), "invalid").futureValue should ===(LocationServiceError(errorMsg))
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
        override private[utils] def makeAgentClient(loc: AkkaLocation) = agentClient
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

  "provision" must {
    "start required number sequence components on available machines for given subsystems | ESW-346" in {
      val uri                 = new URI("some-uri")
      val eswMachine          = mock[AgentClient]
      val irisMachine         = mock[AgentClient]
      val locationServiceUtil = mock[LocationServiceUtil]
      val eswMachineLocation  = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)
      val irisMachineLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "primary"), Machine)), uri)

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override def makeAgentClient(loc: AkkaLocation): AgentClient =
          if (loc.prefix.subsystem == ESW) eswMachine else irisMachine
      }

      when(eswMachine.spawnSequenceComponent(Prefix(ESW, "ESW_1"), None)).thenReturn(Future.successful(Spawned))
      when(eswMachine.spawnSequenceComponent(Prefix(ESW, "ESW_2"), None)).thenReturn(Future.successful(Spawned))
      when(irisMachine.spawnSequenceComponent(Prefix(IRIS, "IRIS_1"), None)).thenReturn(Future.successful(Spawned))

      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(Future.successful(Right(List(eswMachineLocation, irisMachineLocation))))

      val provisionConfig = ProvisionConfig(Map(ESW -> 2, IRIS -> 1))
      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(eswMachine).spawnSequenceComponent(Prefix(ESW, "ESW_1"), None)
      verify(eswMachine).spawnSequenceComponent(Prefix(ESW, "ESW_2"), None)
      verify(irisMachine).spawnSequenceComponent(Prefix(IRIS, "IRIS_1"), None)
    }

    "equally distribute sequence components across machines | ESW-346" in {
      val uri                 = new URI("some-uri")
      val primaryMachine      = mock[AgentClient]
      val secondaryMachine    = mock[AgentClient]
      val locationServiceUtil = mock[LocationServiceUtil]
      val primaryMachineLoc   = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)
      val secondaryMachineLoc = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "secondary"), Machine)), uri)

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override def makeAgentClient(loc: AkkaLocation): AgentClient =
          if (loc == primaryMachineLoc) primaryMachine else secondaryMachine
      }

      when(primaryMachine.spawnSequenceComponent(Prefix(ESW, "ESW_1"), None)).thenReturn(Future.successful(Spawned))
      when(primaryMachine.spawnSequenceComponent(Prefix(ESW, "ESW_3"), None)).thenReturn(Future.successful(Spawned))
      when(primaryMachine.spawnSequenceComponent(Prefix(ESW, "ESW_5"), None)).thenReturn(Future.successful(Spawned))

      when(secondaryMachine.spawnSequenceComponent(Prefix(ESW, "ESW_2"), None)).thenReturn(Future.successful(Spawned))
      when(secondaryMachine.spawnSequenceComponent(Prefix(ESW, "ESW_4"), None)).thenReturn(Future.successful(Spawned))

      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(Future.successful(Right(List(primaryMachineLoc, secondaryMachineLoc))))

      val provisionConfig = ProvisionConfig(Map(ESW -> 5))
      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(primaryMachine).spawnSequenceComponent(Prefix(ESW, "ESW_1"), None)
      verify(primaryMachine).spawnSequenceComponent(Prefix(ESW, "ESW_3"), None)
      verify(primaryMachine).spawnSequenceComponent(Prefix(ESW, "ESW_5"), None)
      verify(secondaryMachine).spawnSequenceComponent(Prefix(ESW, "ESW_2"), None)
      verify(secondaryMachine).spawnSequenceComponent(Prefix(ESW, "ESW_4"), None)
    }

    "return SpawningSequenceComponentsFailed if agent fails to spawn sequence component | ESW-346" in {
      val uri                 = new URI("some-uri")
      val eswMachine          = mock[AgentClient]
      val locationServiceUtil = mock[LocationServiceUtil]
      val eswMachineLocation  = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
        override def makeAgentClient(loc: AkkaLocation): AgentClient = eswMachine
      }

      val errorMsg = "failed to spawn"
      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(Future.successful(Right(List(eswMachineLocation))))
      when(eswMachine.spawnSequenceComponent(Prefix(ESW, "ESW_1"), None)).thenReturn(Future.successful(Spawned))
      when(eswMachine.spawnSequenceComponent(Prefix(ESW, "ESW_2"), None)).thenReturn(Future.successful(Failed(errorMsg)))

      val provisionConfig = ProvisionConfig(Map(ESW -> 2))
      agentUtil.provision(provisionConfig).futureValue should ===(
        ProvisionResponse.SpawningSequenceComponentsFailed(List(SpawnSequenceComponentFailed(errorMsg)))
      )

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(eswMachine).spawnSequenceComponent(Prefix(ESW, "ESW_1"), None)
      verify(eswMachine).spawnSequenceComponent(Prefix(ESW, "ESW_2"), None)
    }

    "return LocationServiceError if location service gives error | ESW-346" in {
      val setup = new TestSetup()
      import setup._
      val errorMsg = "listing failed"
      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(errorMsg))))

      val provisionConfig = ProvisionConfig(Map(ESW -> 2))
      agentUtil.provision(provisionConfig).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
    }

    "return NoMachineFoundForSubsystems if any subsystem does not have machine available | ESW-346" in {
      val setup = new TestSetup()
      import setup._

      val uri                = new URI("some-uri")
      val eswMachineLocation = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)

      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(Future.successful(Right(List(eswMachineLocation))))

      val provisionConfig = ProvisionConfig(Map(ESW -> 1, IRIS -> 1))
      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionResponse.NoMachineFoundForSubsystems(Set(IRIS)))

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
    }
  }

  "getSequenceComponentsRunningOn" must {
    "return all running sequence components on all agents | ESW-349" in {
      val testSetup = new TestSetup()
      import testSetup._

      // machine running esw seq comps
      val eswAgent: AkkaLocation =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "machine1"), Machine)), new URI("some-uri"))

      // machine running Redis
      val cswAgent: AkkaLocation =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(CSW, "machine1"), Machine)), new URI("some-uri"))

      val eswAgentClient = mock[AgentClient]
      val cswAgentClient = mock[AgentClient]

      val eswAgentStatus: AgentStatus = AgentStatus(Map(eswPrimarySeqCompId -> Running, eswSecondarySeqCompId -> Initializing))
      val cswAgentStatus: AgentStatus = AgentStatus(Map(ComponentId(Prefix(CSW, "redis"), Service) -> Running))

      val agentUtil = new AgentUtil(locationServiceUtil) {
        override private[utils] def makeAgentClient(loc: AkkaLocation) = {
          loc.prefix.subsystem match {
            case ESW => eswAgentClient
            case CSW => cswAgentClient
            case _   => agentClient
          }
        }
      }

      when(eswAgentClient.getAgentStatus).thenReturn(Future.successful(eswAgentStatus))
      when(cswAgentClient.getAgentStatus).thenReturn(Future.successful(cswAgentStatus))

      // expected map will include sequence components which are in running state
      val expectedResponse: Map[ComponentId, List[ComponentId]] = Map(
        eswAgent.connection.componentId -> List(eswPrimarySeqCompId),
        cswAgent.connection.componentId -> List()
      )

      val seqComponents = agentUtil.getSequenceComponentsRunningOn(List(eswAgent, cswAgent)).futureValue

      seqComponents should ===(expectedResponse)
    }

    "return empty list when no agent running | ESW-349" in {
      val setup = new TestSetup()
      import setup._

      val expectedResponse: Map[ComponentId, List[ComponentId]] = Map()

      val seqComponents = agentUtil.getSequenceComponentsRunningOn(List.empty).futureValue

      seqComponents should ===(expectedResponse)
    }
  }

  class TestSetup() {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
    val agentClient: AgentClient                 = mock[AgentClient]

    val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil) {
      override private[sm] def getAgent(prefix: Prefix): Future[Either[LocationServiceError, AgentClient]] =
        Future.successful(Right(agentClient))
    }

    val eswPrimarySeqCompId: ComponentId = ComponentId(Prefix(ESW, "primary"), SequenceComponent)
    val eswPrimarySeqCompLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(eswPrimarySeqCompId), new URI("some-uri"))

    val eswSecondarySeqCompId: ComponentId = ComponentId(Prefix(ESW, "secondary"), SequenceComponent)
    val eswSecondarySeqCompLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(eswSecondarySeqCompId), new URI("some-uri"))

    def mockSpawnComponent(response: SpawnResponse): Unit =
      when(agentClient.spawnSequenceComponent(any[Prefix], any[Option[String]]))
        .thenReturn(Future.successful(response))

    def verifySpawnSequenceComponentCalled(): Unit =
      verify(agentClient).spawnSequenceComponent(any[Prefix], any[Option[String]])
  }
}
