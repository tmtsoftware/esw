package esw.agent.service.impl

import java.net.URI

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.{ESW, IRIS, TCS}
import esw.agent.service.api.models.AgentStatusResponse.{LocationServiceError, Success}
import esw.agent.service.api.models.{AgentStatus, SequenceComponentStatus}
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.testcommons.BaseTestSuite

class AgentStatusUtilTest extends BaseTestSuite {
  private implicit val testSystem: ActorSystem[_] = ActorSystem(SpawnProtocol(), "test")

  override protected def afterAll(): Unit = {
    super.afterAll()
    testSystem.terminate()
  }

  "getAllAgentStatus" must {
    "return agent status successfully | ESW-349, ESW-367, ESW-481" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
      val agentStatusUtil: AgentStatusUtil         = new AgentStatusUtil(locationServiceUtil)

      // esw machine => (happy)
      //            eswSeqComp1 => eswSeqCompSequencer1
      //            eswSeqComp2 => None
      val eswMachinePrefix = Prefix(ESW, "machine1")
      val eswMachine       = ComponentId(eswMachinePrefix, Machine)
      val eswMachineLoc    = AkkaLocation(AkkaConnection(eswMachine), URI.create("uri"), Metadata.empty)

      val seqComp1Prefix = Prefix(ESW, "primary")
      val eswSeqComp1    = ComponentId(seqComp1Prefix, SequenceComponent)
      val eswSeqCompLoc1 = seqCompLocationWithAgentPrefix(eswSeqComp1, Some(eswMachinePrefix))
      val eswSequencer1 =
        sequencerLocationWithSeqCompPrefix(ComponentId(Prefix(ESW, "darkNight"), Sequencer), Some(seqComp1Prefix))
      val eswSeqComp1Status = SequenceComponentStatus(eswSeqComp1, Some(eswSequencer1))

      val eswSeqComp2       = ComponentId(Prefix(ESW, "secondary"), SequenceComponent)
      val eswSeqCompLoc2    = seqCompLocationWithAgentPrefix(eswSeqComp2, Some(eswMachinePrefix))
      val eswSeqComp2Status = SequenceComponentStatus(eswSeqComp2, None)

      // Manually started seq comp => (agent prefix not present)
      //            irisSeqComp => None
      val irisPrefix        = Prefix(IRIS, "secondary")
      val irisSeqComp       = ComponentId(irisPrefix, SequenceComponent)
      val irisSeqCompLoc    = seqCompLocationWithAgentPrefix(irisSeqComp, None)
      val irisSeqCompStatus = SequenceComponentStatus(irisSeqComp, None)
      // tcsSeqComp => Some(tcs, darknight) sequencer
      val tcsSeqCompPrefix = Prefix(TCS, "secondary")
      val tcsSeqComp       = ComponentId(tcsSeqCompPrefix, SequenceComponent)
      val tcsSeqCompLoc    = seqCompLocationWithAgentPrefix(tcsSeqComp, None)
      val tcsSequencer1 =
        sequencerLocationWithSeqCompPrefix(ComponentId(Prefix(TCS, "darknight"), Sequencer), Some(tcsSeqCompPrefix))
      val tcsSeqCompStatus = SequenceComponentStatus(tcsSeqComp, Some(tcsSequencer1))

      // TCS Machine => (no seq comp)
      val tcsMachinePrefix = Prefix(TCS, "primary")
      val tcsMachine       = ComponentId(tcsMachinePrefix, Machine)
      val tcsMachineLoc    = seqCompLocationWithAgentPrefix(tcsMachine, None)

      when(locationServiceUtil.listAkkaLocationsBy(Machine))
        .thenReturn(futureRight(List(eswMachineLoc, tcsMachineLoc)))

      when(locationServiceUtil.listAkkaLocationsBy(SequenceComponent))
        .thenReturn(futureRight(List(eswSeqCompLoc1, eswSeqCompLoc2, irisSeqCompLoc, tcsSeqCompLoc)))

      when(locationServiceUtil.listAkkaLocationsBy(Sequencer))
        .thenReturn(futureRight(List(eswSequencer1, tcsSequencer1)))

      val expectedAgentStatus = List(
        AgentStatus(eswMachine, List(eswSeqComp1Status, eswSeqComp2Status)),
        AgentStatus(tcsMachine, List.empty)
      )

      val status = agentStatusUtil.getAllAgentStatus.futureValue.asInstanceOf[Success]
      status.agentStatus.toSet should ===(expectedAgentStatus.toSet)
      status.seqCompsWithoutAgent.toSet should ===(List(irisSeqCompStatus, tcsSeqCompStatus).toSet)

      verify(locationServiceUtil).listAkkaLocationsBy(SequenceComponent)
      verify(locationServiceUtil).listAkkaLocationsBy(Machine)
      verify(locationServiceUtil).listAkkaLocationsBy(Sequencer)
    }

    "return LocationServiceError if location service gives error | ESW-349, ESW-481" in {
      val locationServiceUtil: LocationServiceUtil = mock[LocationServiceUtil]
      val agentStatusUtil: AgentStatusUtil         = new AgentStatusUtil(locationServiceUtil)

      when(locationServiceUtil.listAkkaLocationsBy(SequenceComponent)).thenReturn(futureLeft(RegistrationListingFailed("error")))
      agentStatusUtil.getAllAgentStatus.futureValue should ===(LocationServiceError("error"))
      verify(locationServiceUtil).listAkkaLocationsBy(SequenceComponent)
    }

  }

  private def seqCompLocationWithAgentPrefix(componentId: ComponentId, agentPrefix: Option[Prefix]) =
    AkkaLocation(
      AkkaConnection(componentId),
      URI.create("uri"),
      agentPrefix.fold(Metadata.empty)(p => Metadata().withAgentPrefix(p))
    )

  private def sequencerLocationWithSeqCompPrefix(componentId: ComponentId, seqCompPrefix: Option[Prefix]) =
    AkkaLocation(
      AkkaConnection(componentId),
      URI.create("uri"),
      seqCompPrefix.fold(Metadata.empty)(p => Metadata().withSequenceComponentPrefix(p))
    )
}
