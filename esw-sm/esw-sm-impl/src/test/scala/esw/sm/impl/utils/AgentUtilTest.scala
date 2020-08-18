package esw.sm.impl.utils

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, SpawnResponse, Spawned}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.models.AgentStatusResponses.{AgentSeqCompsStatus, SequenceComponentStatus}
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.AgentStatusResponse.Success
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse
import esw.sm.api.protocol.ProvisionResponse.CouldNotFindMachines
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

  "getAgent" must {
    "return AgentClient associated to ESW machine | ESW-337" in {
      val locationServiceUtil                          = mock[LocationServiceUtil]
      val agentAllocator                               = mock[AgentAllocator]
      val agentClient                                  = mock[AgentClient]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]
      val agentPrefix                                  = Prefix(ESW, "primary")
      val connection                                   = AkkaConnection(ComponentId(agentPrefix, Machine))
      val location                                     = AkkaLocation(connection, new URI("mock"), Metadata.empty)

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
    val eswSeqComp1Name    = randomString(10)
    val eswSeqComp2Name    = randomString(10)
    val irisSeqComp1Name   = randomString(10)
    val eswSeqComp1Prefix  = Prefix(ESW, eswSeqComp1Name)
    val eswSeqComp2Prefix  = Prefix(ESW, eswSeqComp2Name)
    val irisSeqComp1Prefix = Prefix(IRIS, irisSeqComp1Name)

    val eswPrimaryMachine  = akkaLocation(ComponentId(Prefix(ESW, "primary"), Machine), None)
    val irisPrimaryMachine = akkaLocation(ComponentId(Prefix(IRIS, "primary"), Machine), None)

    "start required number sequence components on available machines for given subsystems | ESW-347" in {
      val locationServiceUtil                          = mock[LocationServiceUtil]
      val agentAllocator                               = mock[AgentAllocator]
      val eswClient                                    = mock[AgentClient]
      val irisClient                                   = mock[AgentClient]
      val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator) {
        override def makeAgentClient(loc: AkkaLocation): AgentClient =
          if (loc.prefix.subsystem == ESW) eswClient else irisClient
      }

      val provisionConfig = ProvisionConfig(eswPrimaryMachine.prefix -> 1, irisPrimaryMachine.prefix -> 1)
      val machines        = List(eswPrimaryMachine, irisPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, irisPrimaryMachine -> irisSeqComp1Prefix)

      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(eswClient.spawnSequenceComponent(eswSeqComp1Name, None)).thenReturn(Future.successful(Spawned))
      when(irisClient.spawnSequenceComponent(irisSeqComp1Name, None)).thenReturn(Future.successful(Spawned))

      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
      verify(eswClient).spawnSequenceComponent(eswSeqComp1Name, None)
      verify(irisClient).spawnSequenceComponent(irisSeqComp1Name, None)
    }

    "return SpawningSequenceComponentsFailed if agent fails to spawn sequence component | ESW-347" in {
      val setup = new TestSetup()
      import setup._

      val errorMsg        = "failed to spawn"
      val provisionConfig = ProvisionConfig(eswPrimaryMachine.prefix -> 2)
      val machines        = List(eswPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, eswPrimaryMachine -> eswSeqComp2Prefix)

      when(locationServiceUtil.listAkkaLocationsBy(Machine)).thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(agentClient.spawnSequenceComponent(eswSeqComp1Name, None)).thenReturn(Future.successful(Spawned))
      when(agentClient.spawnSequenceComponent(eswSeqComp2Name, None)).thenReturn(Future.successful(Failed(errorMsg)))

      val response = agentUtil.provision(provisionConfig).futureValue
      response shouldBe a[ProvisionResponse.SpawningSequenceComponentsFailed]
      val failureMgs = response.asInstanceOf[ProvisionResponse.SpawningSequenceComponentsFailed].failureResponses.head
      // assert that failure msg has necessary info
      failureMgs.contains(eswPrimaryMachine.prefix.toString()) shouldBe true
      failureMgs.contains(eswSeqComp2Name) shouldBe true
      failureMgs.contains(errorMsg) shouldBe true

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
      verify(agentClient).spawnSequenceComponent(eswSeqComp1Name, None)
      verify(agentClient).spawnSequenceComponent(eswSeqComp2Name, None)
    }

    "return LocationServiceError if location service gives error | ESW-347" in {
      val setup = new TestSetup()
      import setup._
      val errorMsg = "listing failed"
      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(errorMsg))))

      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 2)
      agentUtil.provision(provisionConfig).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
    }

    "return CouldNotFindMachines error if any subsystem does not have machine available | ESW-347" in {
      val setup = new TestSetup()
      import setup._
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 1, Prefix(IRIS, "primary") -> 1)
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

      val agentUtil: AgentUtil = new AgentUtil(locationServiceUtil, sequenceComponentUtil, agentAllocator)

      // esw machine => (happy)
      //            eswSeqComp1 => eswSeqCompSequencer1
      //            eswSeqComp2 => None
      val eswMachinePrefix     = Prefix(ESW, "machine1")
      val eswMachine           = ComponentId(eswMachinePrefix, Machine)
      val eswMachineLoc        = akkaLocation(eswMachine, None)
      val eswSeqComp1          = ComponentId(Prefix(ESW, "primary"), SequenceComponent)
      val eswSeqCompLoc1       = akkaLocation(eswSeqComp1, Some(eswMachinePrefix))
      val eswSeqCompSequencer1 = akkaLocation(ComponentId(Prefix(ESW, "darkNight"), Sequencer), None)
      val eswSeqComp1Status    = SequenceComponentStatus(eswSeqComp1, Some(eswSeqCompSequencer1))

      val eswSeqComp2       = ComponentId(Prefix(ESW, "secondary"), SequenceComponent)
      val eswSeqCompLoc2    = akkaLocation(eswSeqComp2, Some(eswMachinePrefix))
      val eswSeqComp2Status = SequenceComponentStatus(eswSeqComp2, None)

      // Unknown Machine => (agent prefix not present)
      //            irisSeqComp => None
      val irisPrefix        = Prefix(IRIS, "secondary")
      val unknownMachine    = ComponentId(Prefix(s"${irisPrefix.subsystem}.Unknown"), Machine)
      val unknownMachineLoc = akkaLocation(unknownMachine, None)
      val irisSeqComp       = ComponentId(irisPrefix, SequenceComponent)
      val irisSeqCompLoc    = akkaLocation(irisSeqComp, None)
      val irisSeqCompStatus = SequenceComponentStatus(irisSeqComp, None)

      // TCS Machine => (no seq comp)
      val tcsMachinePrefix = Prefix(TCS, "primary")
      val tcsMachine       = ComponentId(tcsMachinePrefix, Machine)
      val tcsMachineLoc    = akkaLocation(tcsMachine, None)

      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(futureRight(List(eswMachineLoc, unknownMachineLoc, tcsMachineLoc)))

      when(locationServiceUtil.listAkkaLocationsBy(SequenceComponent))
        .thenReturn(futureRight(List(eswSeqCompLoc1, eswSeqCompLoc2, irisSeqCompLoc)))

      when(sequenceComponentUtil.getSequenceComponentStatus(eswSeqCompLoc1))
        .thenReturn(Future.successful(eswSeqComp1Status))
      when(sequenceComponentUtil.getSequenceComponentStatus(eswSeqCompLoc2))
        .thenReturn(Future.successful(eswSeqComp2Status))
      when(sequenceComponentUtil.getSequenceComponentStatus(irisSeqCompLoc))
        .thenReturn(Future.successful(irisSeqCompStatus))

      val expectedStatus = List(
        AgentSeqCompsStatus(eswMachine, List(eswSeqComp1Status, eswSeqComp2Status)),
        AgentSeqCompsStatus(unknownMachine, List(irisSeqCompStatus)),
        AgentSeqCompsStatus(tcsMachine, List.empty)
      )

      agentUtil.getAllAgentStatus.futureValue.asInstanceOf[Success].response.toSet should ===(expectedStatus.toSet)

      verify(locationServiceUtil).listAkkaLocationsBy(SequenceComponent)
      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(sequenceComponentUtil).getSequenceComponentStatus(eswSeqCompLoc1)
      verify(sequenceComponentUtil).getSequenceComponentStatus(eswSeqCompLoc2)
      verify(sequenceComponentUtil).getSequenceComponentStatus(irisSeqCompLoc)
    }

    "return LocationServiceError if location service gives error | ESW-349" in {
      val setup = new TestSetup()
      import setup._

      when(locationServiceUtil.listAkkaLocationsBy(SequenceComponent)).thenReturn(futureLeft(RegistrationListingFailed("error")))
      agentUtil.getAllAgentStatus.futureValue should ===(LocationServiceError("error"))
      verify(locationServiceUtil).listAkkaLocationsBy(SequenceComponent)
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
      AkkaLocation(AkkaConnection(eswPrimarySeqCompId), new URI("some-uri"), Metadata.empty)

    val eswSecondarySeqCompId: ComponentId = ComponentId(Prefix(ESW, "secondary"), SequenceComponent)
    val eswSecondarySeqCompLocation: AkkaLocation =
      AkkaLocation(AkkaConnection(eswSecondarySeqCompId), new URI("some-uri"), Metadata.empty)

    def mockSpawnComponent(response: SpawnResponse): Unit =
      when(agentClient.spawnSequenceComponent(any[String], any[Option[String]]))
        .thenReturn(Future.successful(response))

    def verifySpawnSequenceComponentCalled(): Unit =
      verify(agentClient).spawnSequenceComponent(any[String], any[Option[String]])
  }

  private def akkaLocation(componentId: ComponentId, agentPrefix: Option[Prefix]) =
    AkkaLocation(
      AkkaConnection(componentId),
      URI.create("uri"),
      agentPrefix.fold(Metadata.empty)(p => Metadata().withAgentPrefix(p))
    )
}
