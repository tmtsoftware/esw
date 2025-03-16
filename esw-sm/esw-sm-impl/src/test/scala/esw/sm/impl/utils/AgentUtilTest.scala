package esw.sm.impl.utils

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.util.Timeout
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS}
import esw.agent.pekko.client.AgentClient
import esw.agent.service.api.models.{Failed, SpawnResponse, Spawned}
import esw.commons.utils.config.{FetchingScriptVersionFailed, VersionManager}
import esw.commons.utils.location.EswLocationError.{LocationNotFound, RegistrationListingFailed}
import esw.commons.utils.location.LocationServiceUtil
import org.mockito.ArgumentMatchers.{any, eq => argEq}
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse
import esw.sm.api.protocol.ProvisionResponse.{CouldNotFindMachines, ProvisionVersionFailure}
import esw.testcommons.BaseTestSuite
import org.mockito.Mockito.{verify, when}

import java.net.URI
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class AgentUtilTest extends BaseTestSuite {
  implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")
  implicit val timeout: Timeout                                = 1.hour

  private val versionManager: VersionManager = mock[VersionManager]

  override def afterAll(): Unit = {
    actorSystem.terminate()
    actorSystem.whenTerminated.futureValue
  }

  "getAgent" must {
    "return AgentClient associated to ESW machine | ESW-337" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentAllocator      = mock[AgentAllocator]
      val agentClient         = mock[AgentClient]
      val agentPrefix         = Prefix(ESW, "primary")
      val connection          = PekkoConnection(ComponentId(agentPrefix, Machine))
      val location            = PekkoLocation(connection, new URI("mock"), Metadata.empty)

      when(locationServiceUtil.find(connection)).thenReturn(futureRight(location))

      val agentUtil = new AgentUtil(locationServiceUtil, agentAllocator, versionManager) {
        override private[utils] def makeAgentClient(agentLocation: PekkoLocation) = agentClient
      }

      agentUtil.getAndMakeAgentClient(agentPrefix).rightValue should ===(agentClient)
      verify(locationServiceUtil).find(connection)
    }

    "return LocationNotFound when location service find call returns LocationNotFound | ESW-337" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentAllocator      = mock[AgentAllocator]
      val agentPrefix         = Prefix(ESW, "primary")
      val connection          = PekkoConnection(ComponentId(agentPrefix, Machine))
      val locationNotFound    = LocationNotFound("location not found")

      when(locationServiceUtil.find(connection)).thenReturn(futureLeft(locationNotFound))

      val agentUtil = new AgentUtil(locationServiceUtil, agentAllocator, versionManager)
      agentUtil.getAndMakeAgentClient(agentPrefix).leftValue should ===(LocationServiceError(locationNotFound.msg))

      verify(locationServiceUtil).find(connection)
    }

    "return RegistrationListingFailed when location service find call returns RegistrationListingFailed | ESW-337" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentAllocator      = mock[AgentAllocator]
      val agentPrefix         = Prefix(ESW, "primary")
      val connection          = PekkoConnection(ComponentId(agentPrefix, Machine))
      val listingFailed       = RegistrationListingFailed("listing failed")

      when(locationServiceUtil.find(connection)).thenReturn(futureLeft(listingFailed))

      val agentUtil = new AgentUtil(locationServiceUtil, agentAllocator, versionManager)
      agentUtil.getAndMakeAgentClient(agentPrefix).leftValue should ===(LocationServiceError(listingFailed.msg))

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

    val eswPrimaryMachine  = seqCompLocationWithAgentPrefix(ComponentId(Prefix(ESW, "primary"), Machine), None)
    val irisPrimaryMachine = seqCompLocationWithAgentPrefix(ComponentId(Prefix(IRIS, "primary"), Machine), None)

    "start required number sequence components on available machines for given subsystems | ESW-347" in {
      val locationServiceUtil = mock[LocationServiceUtil]
      val agentAllocator      = mock[AgentAllocator]
      val eswClient           = mock[AgentClient]
      val irisClient          = mock[AgentClient]

      val agentUtil: AgentUtil =
        new AgentUtil(locationServiceUtil, agentAllocator, versionManager) {
          override def makeAgentClient(agentLocation: PekkoLocation): AgentClient =
            if (agentLocation.prefix.subsystem == ESW) eswClient else irisClient
        }

      val provisionConfig = ProvisionConfig(eswPrimaryMachine.prefix -> 1, irisPrimaryMachine.prefix -> 1)
      val machines        = List(eswPrimaryMachine, irisPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, irisPrimaryMachine -> irisSeqComp1Prefix)
      val version         = randomString(10)

      when(locationServiceUtil.listPekkoLocationsBy(argEq(Machine), any[PekkoLocation => Boolean]))
        .thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(eswClient.spawnSequenceComponent(eswSeqComp1Name, Some(version), simulation = false))
        .thenReturn(Future.successful(Spawned))
      when(irisClient.spawnSequenceComponent(irisSeqComp1Name, Some(version))).thenReturn(Future.successful(Spawned))
      when(versionManager.getScriptVersion).thenReturn(Future.successful(version))

      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionResponse.Success)

      verify(locationServiceUtil).listPekkoLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
      verify(eswClient).spawnSequenceComponent(eswSeqComp1Name, Some(version), false)
      verify(irisClient).spawnSequenceComponent(irisSeqComp1Name, Some(version), false)
    }

    "return SpawningSequenceComponentsFailed if agent fails to spawn sequence component | ESW-347" in {
      val setup = new TestSetup()
      import setup.*

      val errorMsg        = "failed to spawn"
      val provisionConfig = ProvisionConfig(eswPrimaryMachine.prefix -> 2)
      val machines        = List(eswPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, eswPrimaryMachine -> eswSeqComp2Prefix)
      val version         = randomString(10)

      when(locationServiceUtil.listPekkoLocationsBy(argEq(Machine), any[PekkoLocation => Boolean]))
        .thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(agentClient.spawnSequenceComponent(eswSeqComp1Name, Some(version), false))
        .thenReturn(Future.successful(Spawned))
      when(agentClient.spawnSequenceComponent(eswSeqComp2Name, Some(version), false))
        .thenReturn(Future.successful(Failed(errorMsg)))
      when(versionManager.getScriptVersion).thenReturn(Future.successful(version))

      val response = agentUtil.provision(provisionConfig).futureValue
      response shouldBe a[ProvisionResponse.SpawningSequenceComponentsFailed]
      val failureMgs = response.asInstanceOf[ProvisionResponse.SpawningSequenceComponentsFailed].failureResponses.head
      // assert that failure msg has necessary info
      failureMgs.contains(eswPrimaryMachine.prefix.toString()) shouldBe true
      failureMgs.contains(eswSeqComp2Name) shouldBe true
      failureMgs.contains(errorMsg) shouldBe true

      verify(locationServiceUtil).listPekkoLocationsBy(Machine)
      verify(agentAllocator).allocate(provisionConfig, machines)
      verify(agentClient).spawnSequenceComponent(eswSeqComp1Name, Some(version), false)
      verify(agentClient).spawnSequenceComponent(eswSeqComp2Name, Some(version), false)
    }

    "return LocationServiceError if location service gives error | ESW-347" in {
      val setup = new TestSetup()
      import setup.*
      val errorMsg = "listing failed"
      when(locationServiceUtil.listPekkoLocationsBy(argEq(Machine), any[PekkoLocation => Boolean]))
        .thenReturn(Future.successful(Left(RegistrationListingFailed(errorMsg))))

      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 2)
      agentUtil.provision(provisionConfig).futureValue should ===(LocationServiceError(errorMsg))

      verify(locationServiceUtil).listPekkoLocationsBy(argEq(Machine), any[PekkoLocation => Boolean])
    }

    "return CouldNotFindMachines error if any subsystem does not have machine available | ESW-347" in {
      val setup = new TestSetup()
      import setup.*
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 1, Prefix(IRIS, "primary") -> 1)
      val machines        = List(eswPrimaryMachine)
      val error           = CouldNotFindMachines(Set(Prefix(IRIS, "primary")))
      when(locationServiceUtil.listPekkoLocationsBy(argEq(Machine), any[PekkoLocation => Boolean]))
        .thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Left(error))

      agentUtil.provision(provisionConfig).futureValue should ===(error)
      verify(locationServiceUtil).listPekkoLocationsBy(argEq(Machine), any[PekkoLocation => Boolean])
      verify(agentAllocator).allocate(provisionConfig, machines)
    }

    "return ProvisionVersionFailure error if versionConf is not present | ESW-360" in {
      val setup = new TestSetup()
      import setup.*
      val provisionConfig = ProvisionConfig(Prefix(ESW, "primary") -> 1, Prefix(IRIS, "primary") -> 1)
      val machines        = List(eswPrimaryMachine)
      val mapping         = List(eswPrimaryMachine -> eswSeqComp1Prefix, eswPrimaryMachine -> eswSeqComp2Prefix)
      val errorMsg        = randomString(10)

      when(locationServiceUtil.listPekkoLocationsBy(Machine)).thenReturn(futureRight(machines))
      when(agentAllocator.allocate(provisionConfig, machines)).thenReturn(Right(mapping))
      when(versionManager.getScriptVersion).thenReturn(Future.failed(FetchingScriptVersionFailed(errorMsg)))

      agentUtil.provision(provisionConfig).futureValue should ===(ProvisionVersionFailure(errorMsg))
    }
  }

  class TestSetup() {
    val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
    val agentAllocator: AgentAllocator           = mock[AgentAllocator]
    val agentClient: AgentClient                 = mock[AgentClient]

    val agentUtil: AgentUtil =
      new AgentUtil(locationServiceUtil, agentAllocator, versionManager) {
        override private[sm] def getAndMakeAgentClient(agentPrefix: Prefix) = futureRight(agentClient)

        override private[utils] def makeAgentClient(agentLocation: PekkoLocation) = agentClient
      }

    val eswPrimarySeqCompId: ComponentId = ComponentId(Prefix(ESW, "primary"), SequenceComponent)
    val eswPrimarySeqCompLocation: PekkoLocation =
      PekkoLocation(PekkoConnection(eswPrimarySeqCompId), new URI("some-uri"), Metadata.empty)

    val eswSecondarySeqCompId: ComponentId = ComponentId(Prefix(ESW, "secondary"), SequenceComponent)
    val eswSecondarySeqCompLocation: PekkoLocation =
      PekkoLocation(PekkoConnection(eswSecondarySeqCompId), new URI("some-uri"), Metadata.empty)

    def mockSpawnComponent(response: SpawnResponse): Unit =
      when(agentClient.spawnSequenceComponent(any[String], any[Option[String]]))
        .thenReturn(Future.successful(response))

    def verifySpawnSequenceComponentCalled(): Unit =
      verify(agentClient).spawnSequenceComponent(any[String], any[Option[String]])
  }

  private def seqCompLocationWithAgentPrefix(componentId: ComponentId, agentPrefix: Option[Prefix]) =
    PekkoLocation(
      PekkoConnection(componentId),
      URI.create("uri"),
      agentPrefix.fold(Metadata.empty)(p => Metadata().withAgentPrefix(p))
    )

}
