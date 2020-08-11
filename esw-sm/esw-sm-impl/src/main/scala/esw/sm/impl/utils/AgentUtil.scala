package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{AgentStatus, ComponentStatus, Failed, Spawned}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.models.AgentStatusResponses.{AgentSeqCompsStatus, AgentToSeqCompsMap}
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse.SpawningSequenceComponentsFailed
import esw.sm.api.protocol.SpawnSequenceComponentResponse.{SpawnSequenceComponentFailed, Success}
import esw.sm.api.protocol.{AgentStatusResponse, ProvisionResponse, SpawnSequenceComponentResponse}

import scala.concurrent.Future

class AgentUtil(
    locationServiceUtil: LocationServiceUtil,
    sequenceComponentUtil: SequenceComponentUtil,
    agentAllocator: AgentAllocator
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def getAllAgentStatus: Future[AgentStatusResponse] =
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .flatMapRight(getSequenceComponentsRunningOn(_).flatMap(getAllSeqCompsStatus))
      .mapToAdt(agentStatusList => AgentStatusResponse.Success(agentStatusList), e => LocationServiceError(e.msg))

  private def getAllSeqCompsStatus(agentToSeqCompsList: List[AgentToSeqCompsMap]): Future[List[AgentSeqCompsStatus]] = {
    Future.traverse(agentToSeqCompsList) { agentToSeqComp =>
      sequenceComponentUtil
        .getSequenceComponentStatus(agentToSeqComp.seqComps)
        .map(seqCompStatus => AgentSeqCompsStatus(agentToSeqComp.agentId, seqCompStatus))
    }
  }

  def spawnSequenceComponent(machine: Prefix, seqCompName: String): Future[SpawnSequenceComponentResponse] =
    getAgent(machine).flatMapE(spawnSeqComp(machine, _, Prefix(machine.subsystem, seqCompName))).mapToAdt(identity, identity)

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] =
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .mapLeft(e => LocationServiceError(e.msg))
      .flatMapToAdt(provisionOn(_, provisionConfig), identity)

  private def getSequenceComponentsRunningOn(agents: List[AkkaLocation]): Future[List[AgentToSeqCompsMap]] =
    Future
      .traverse(agents) { agent =>
        makeAgentClient(agent).getAgentStatus
          .map(filterRunningSeqComps)
          .map(seqComps => AgentToSeqCompsMap(agent.connection.componentId, seqComps))
      }

  private def provisionOn(machines: List[AkkaLocation], provisionConfig: ProvisionConfig) =
    Future
      .successful(agentAllocator.allocate(provisionConfig, machines))
      .flatMapRight(spawnComponentsByMapping)
      .mapToAdt(identity, identity)

  private def spawnComponentsByMapping(mappings: List[(AkkaLocation, Prefix)]): Future[ProvisionResponse] =
    Future
      .traverse(mappings) {
        case (machine, seqCompPrefix) =>
          spawnSeqComp(machine.prefix, makeAgentClient(machine), seqCompPrefix).map(_.fold(e => Some(e.msg), _ => None))
      }
      .map(_.flatten)
      .map(errs => if (errs.isEmpty) ProvisionResponse.Success else SpawningSequenceComponentsFailed(errs))

  private def spawnSeqComp(machine: Prefix, agentClient: AgentClient, seqCompPrefix: Prefix) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix)
      .map {
        case Spawned => Right(Success(ComponentId(seqCompPrefix, SequenceComponent)))
        case Failed(msg) =>
          Left(
            SpawnSequenceComponentFailed(s"Failed to spawn Sequence component: $seqCompPrefix on Machine: $machine, reason: $msg")
          )
      }

  private[utils] def getAgent(prefix: Prefix): Future[Either[LocationServiceError, AgentClient]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, Machine)))
      .mapRight(location => makeAgentClient(location))
      .mapLeft(error => LocationServiceError(error.msg))

  private[utils] def makeAgentClient(loc: AkkaLocation): AgentClient = new AgentClient(loc)

  private def filterRunningSeqComps(agentStatus: AgentStatus) =
    agentStatus.statuses.filter { case (compId, status) => isRunningSeqComp(compId, status) }.keys.toList

  private def isRunningSeqComp(compId: ComponentId, status: ComponentStatus) =
    compId.componentType == SequenceComponent && status == ComponentStatus.Running
}
