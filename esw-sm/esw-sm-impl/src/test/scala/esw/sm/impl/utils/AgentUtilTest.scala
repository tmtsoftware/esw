package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer, Service}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{CSW, ESW, IRIS, TCS}
import esw.agent.api.ComponentStatus.{Initializing, Running}
import esw.agent.api.{AgentStatus, Failed, SpawnResponse, Spawned}
import esw.agent.client.AgentClient
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.models.AgentStatusResponses.{AgentSeqCompsStatus, SequenceComponentStatus}
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse.CouldNotFindMachines
import esw.sm.api.protocol.SpawnSequenceComponentResponse.SpawnSequenceComponentFailed
import esw.sm.api.protocol.{AgentStatusResponse, ProvisionResponse, SpawnSequenceComponentResponse}
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
      val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
      val agentAllocator: AgentAllocator               = mock[AgentAllocator]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]

      val errorMsg = "Error in agent"
      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator) {
        override private[sm] def getAgent(prefix: Prefix): Future[Either[LocationServiceError, AgentClient]] =
          futureLeft(LocationServiceError(errorMsg))
      }

      agentUtil.spawnSequenceComponent(Prefix(ESW, "invalid"), "invalid").futureValue should ===(LocationServiceError(errorMsg))
    }
  }

  "getAgent" must {
    "return AgentClient associated to ESW machine | ESW-337" in {
      val locationServiceUtil                          = mock[LocationServiceUtil]
      val agentAllocator                               = mock[AgentAllocator]
      val agentClient                                  = mock[AgentClient]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
      val agentPrefix                                  = Prefix(ESW, "primary")
      val connection                                   = AkkaConnection(ComponentId(agentPrefix, Machine))
      val location                                     = AkkaLocation(connection, new URI("mock"))

      when(locationServiceUtil.find(connection)).thenReturn(futureRight(location))

      val agentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator) {
        override private[utils] def makeAgentClient(loc: AkkaLocation) = agentClient
      }

      agentUtil.getAgent(agentPrefix).rightValue should ===(agentClient)
      verify(locationServiceUtil).find(connection)
    }

    "return LocationNotFound when location service find call returns LocationNotFound | ESW-337" in {
      val locationServiceUtil                          = mock[LocationServiceUtil]
      val agentAllocator                               = mock[AgentAllocator]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
      val agentPrefix                                  = Prefix(ESW, "primary")
      val connection                                   = AkkaConnection(ComponentId(agentPrefix, Machine))
      val locationNotFound                             = LocationNotFound("location not found")

      when(locationServiceUtil.find(connection)).thenReturn(futureLeft(locationNotFound))

      val agentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator)
      agentUtil.getAgent(agentPrefix).leftValue should ===(LocationServiceError(locationNotFound.msg))

      verify(locationServiceUtil).find(connection)
    }

    "return RegistrationListingFailed when location service find call returns RegistrationListingFailed | ESW-337" in {
      val locationServiceUtil                          = mock[LocationServiceUtil]
      val agentAllocator                               = mock[AgentAllocator]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
      val agentPrefix                                  = Prefix(ESW, "primary")
      val connection                                   = AkkaConnection(ComponentId(agentPrefix, Machine))
      val listingFailed                                = RegistrationListingFailed("listing failed")

      when(locationServiceUtil.find(connection)).thenReturn(futureLeft(listingFailed))

      val agentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator)
      agentUtil.getAgent(agentPrefix).leftValue should ===(LocationServiceError(listingFailed.msg))

      verify(locationServiceUtil).find(connection)
    }
  }

  "provision" must {
    val uri                = new URI("some-uri")
    val eswSeqComp1Prefix  = Prefix(ESW, "ESW_1")
    val eswSeqComp2Prefix  = Prefix(ESW, "ESW_2")
    val irisSeqComp1Prefix = Prefix(IRIS, "IRIS_1")
    val eswPrimaryMachine  = AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "primary"), Machine)), uri)
    val irisPrimaryMachine = AkkaLocation(AkkaConnection(ComponentId(Prefix(IRIS, "primary"), Machine)), uri)

    "start required number sequence components on available machines for given subsystems | ESW-346" in {
      val locationServiceUtil                          = mock[LocationServiceUtil]
      val agentAllocator                               = mock[AgentAllocator]
      val eswClient                                    = mock[AgentClient]
      val irisClient                                   = mock[AgentClient]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator) {
        override def makeAgentClient(loc: AkkaLocation): AgentClient =
          if (loc.prefix.subsystem == ESW) eswClient else irisClient
      }

      val provisionConfig = ProvisionConfig(Map(eswPrimaryMachine.prefix -> 1, irisPrimaryMachine.prefix -> 1))
      val machines        = List(eswPrimaryMachine, irisPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, irisPrimaryMachine -> irisSeqComp1Prefix)

      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(eswClient.spawnSequenceComponent(eswSeqComp1Prefix, None)).thenReturn(Future.successful(Spawned))
      when(irisClient.spawnSequenceComponent(irisSeqComp1Prefix, None)).thenReturn(Future.successful(Spawned))

      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
      verify(eswClient).spawnSequenceComponent(eswSeqComp1Prefix, None)
      verify(irisClient).spawnSequenceComponent(irisSeqComp1Prefix, None)
    }

    "return SpawningSequenceComponentsFailed if agent fails to spawn sequence component | ESW-346" in {
      val setup = new TestSetup()
      import setup._

      val errorMsg        = "failed to spawn"
      val provisionConfig = ProvisionConfig(Map(eswPrimaryMachine.prefix -> 2))
      val machines        = List(eswPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, eswPrimaryMachine -> eswSeqComp2Prefix)

      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(agentClient.spawnSequenceComponent(eswSeqComp1Prefix, None)).thenReturn(Future.successful(Spawned))
      when(agentClient.spawnSequenceComponent(eswSeqComp2Prefix, None)).thenReturn(Future.successful(Failed(errorMsg)))

      val response = agentUtil.provision(provisionConfig).futureValue
      response shouldBe a[ProvisionResponse.SpawningSequenceComponentsFailed]
      val failureMgs = response.asInstanceOf[ProvisionResponse.SpawningSequenceComponentsFailed].failureResponses.head
      // assert that failure msg has necessary info
      failureMgs.contains(eswPrimaryMachine.prefix.toString()) shouldBe true
      failureMgs.contains(eswSeqComp2Prefix.toString()) shouldBe true
      failureMgs.contains(errorMsg) shouldBe true

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
      verify(agentClient).spawnSequenceComponent(eswSeqComp1Prefix, None)
      verify(agentClient).spawnSequenceComponent(eswSeqComp2Prefix, None)
    }

    "return LocationServiceError if location service gives error | ESW-346" in {
      val setup = new TestSetup()
      import setup._
      val errorMsg = "listing failed"
      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(errorMsg))))

      val provisionConfig = ProvisionConfig(Map(Prefix(ESW, "primary") -> 2))
      agentUtil.provision(provisionConfig).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
    }

    "return NoMachineFoundForSubsystems if any subsystem does not have machine available | ESW-346" in {
      val setup = new TestSetup()
      import setup._
      val provisionConfig = ProvisionConfig(Map(Prefix(ESW, "primary") -> 1, Prefix(IRIS, "primary") -> 1))
      val machines        = List(eswPrimaryMachine)
      val error           = CouldNotFindMachines(Set(Prefix(IRIS, "primary")))
      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Left(error))

      agentUtil.provision(provisionConfig).futureValue should ===(error)
      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
    }
  }

  "getAllAgentStatus" must {
    "return agent status successfully | ESW-349" in {
      val setup = new TestSetup()
      import setup._
      val eswAgentClient  = mock[AgentClient]
      val tcsAgentClient  = mock[AgentClient]
      val irisAgentClient = mock[AgentClient]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator) {
        override private[utils] def makeAgentClient(loc: AkkaLocation) = {
          loc.prefix match {
            case prefix if prefix.subsystem == ESW  => eswAgentClient
            case prefix if prefix.subsystem == TCS  => tcsAgentClient
            case prefix if prefix.subsystem == IRIS => irisAgentClient
          }
        }
      }

      val eswSeqComp1 = ComponentId(Prefix(ESW, "primary"), SequenceComponent)
      val eswSeqComp2 = ComponentId(Prefix(ESW, "secondary"), SequenceComponent)
      val tcsSeqComp1 = ComponentId(Prefix(TCS, "primary"), SequenceComponent)

      when(eswAgentClient.getAgentStatus)
        .thenReturn(Future.successful(AgentStatus(Map(eswSeqComp1 -> Running, eswSeqComp2 -> Initializing))))
      when(tcsAgentClient.getAgentStatus).thenReturn(Future.successful(AgentStatus(Map(tcsSeqComp1 -> Running))))
      when(irisAgentClient.getAgentStatus)
        .thenReturn(Future.successful(AgentStatus(Map(ComponentId(Prefix(CSW, "redis"), Service) -> Running))))

      val eswMachine  = ComponentId(Prefix(ESW, "machine1"), Machine)
      val irisMachine = ComponentId(Prefix(IRIS, "machine1"), Machine)
      val tcsMachine  = ComponentId(Prefix(TCS, "machine1"), Machine)

      val eswMachine1SeqComps =
        List(SequenceComponentStatus(eswSeqComp1, Some(akkaLocation(ComponentId(Prefix(ESW, "darkNight"), Sequencer)))))
      val tcsMachine1SeqComps = List(SequenceComponentStatus(tcsSeqComp1, None))

      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(futureRight(List(akkaLocation(eswMachine), akkaLocation(irisMachine), akkaLocation(tcsMachine))))
      when(sequenceComponentUtil.getSequenceComponentStatus(List(eswSeqComp1))).thenReturn(Future.successful(eswMachine1SeqComps))
      when(sequenceComponentUtil.getSequenceComponentStatus(List(tcsSeqComp1))).thenReturn(Future.successful(tcsMachine1SeqComps))
      when(sequenceComponentUtil.getSequenceComponentStatus(List.empty)).thenReturn(Future.successful(List.empty))

      val expectedStatus = List(
        AgentSeqCompsStatus(eswMachine, eswMachine1SeqComps),
        AgentSeqCompsStatus(tcsMachine, tcsMachine1SeqComps),
        AgentSeqCompsStatus(irisMachine, List.empty)
      )

      val actualResponse = agentUtil.getAllAgentStatus.futureValue
      actualResponse.isInstanceOf[AgentStatusResponse.Success] should ===(true)
      val actualStatus = actualResponse.asInstanceOf[AgentStatusResponse.Success].response
      actualStatus.size should ===(expectedStatus.size)
      actualStatus.diff(expectedStatus) should ===(List.empty)

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(sequenceComponentUtil).getSequenceComponentStatus(List(eswSeqComp1))
      verify(sequenceComponentUtil).getSequenceComponentStatus(List(tcsSeqComp1))
      verify(sequenceComponentUtil).getSequenceComponentStatus(List.empty)
    }

    "return LocationServiceError if location service gives error | ESW-349" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(futureLeft(RegistrationListingFailed("error")))

      agentUtil.getAllAgentStatus.futureValue should ===(LocationServiceError("error"))

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
    }
  }

  class TestSetup() {
    val locationServiceUtil: LocationServiceUtil     = mock[LocationServiceUtil]
    val agentAllocator: AgentAllocator               = mock[AgentAllocator]
    val agentClient: AgentClient                     = mock[AgentClient]
    val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]

    val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator) {
      override private[sm] def getAgent(prefix: Prefix) = futureRight(agentClient)

      override private[utils] def makeAgentClient(loc: AkkaLocation) = agentClient
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

  private def akkaLocation(componentId: ComponentId) = AkkaLocation(AkkaConnection(componentId), URI.create("uri"))
}
