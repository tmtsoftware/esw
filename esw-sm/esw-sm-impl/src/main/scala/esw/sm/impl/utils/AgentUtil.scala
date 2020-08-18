package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Prefix
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, Spawned}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.sm.api.models.AgentStatusResponses.AgentSeqCompsStatus
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse.SpawningSequenceComponentsFailed
import esw.sm.api.protocol.SpawnSequenceComponentResponse.{SpawnSequenceComponentFailed, Success}
import esw.sm.api.protocol.{AgentStatusResponse, ProvisionResponse}

import scala.concurrent.Future

class AgentUtil(
    locationServiceUtil: LocationServiceUtil,
    sequenceComponentUtil: SequenceComponentUtil,
    agentAllocator: AgentAllocator
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def getAllAgentStatus: Future[AgentStatusResponse] =
    locationServiceUtil
      .listAkkaLocationsBy(SequenceComponent)
      .flatMapE(locs => addMissingAgents(locs.groupBy(makeAgentIdFrom)))
      .flatMapRight(getAgentToSeqCompsStatus)
      .mapToAdt(agentStatusList => AgentStatusResponse.Success(agentStatusList), e => LocationServiceError(e.msg))

  private def addMissingAgents(map: Map[ComponentId, List[AkkaLocation]]) =
    getAllAgentIds.mapRight(addKeysIfNotExist(map, _, List.empty))

  def addKeyIfNotExist[K, V](map: Map[K, V], key: K, defaultValue: V): Map[K, V] =
    map.updatedWith(key) {
      case s: Some[_] => s
      case None       => Some(defaultValue)
    }

  def addKeysIfNotExist[K, V](map: Map[K, V], keys: List[K], defaultValue: V): Map[K, V] =
    keys.flatMap(addKeyIfNotExist(map, _, defaultValue)).toMap

  private def getAllAgentIds: Future[Either[EswLocationError, List[ComponentId]]] =
    locationServiceUtil.listAkkaLocationsBy(Machine).mapRight(_.map(_.connection.componentId))

  private def makeAgentIdFrom(seqCompLocation: Location) = {
    val unknownAgent = Prefix(s"${seqCompLocation.connection.componentId.prefix.subsystem}.Unknown")
    val agentPrefix  = seqCompLocation.metadata.getAgentPrefix.getOrElse(unknownAgent)
    ComponentId(agentPrefix, Machine)
  }

  private def getAgentToSeqCompsStatus(agentToSeqComp: Map[ComponentId, List[AkkaLocation]]): Future[List[AgentSeqCompsStatus]] =
    Future.traverse(agentToSeqComp.toList) {
      case (agentId, seqCompLocations) =>
        Future
          .traverse(seqCompLocations)(sequenceComponentUtil.getSequenceComponentStatus)
          .map(AgentSeqCompsStatus(agentId, _))
    }

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] =
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .mapLeft(e => LocationServiceError(e.msg))
      .flatMapToAdt(provisionOn(_, provisionConfig), identity)

  private def provisionOn(machines: List[AkkaLocation], provisionConfig: ProvisionConfig) =
    Future
      .successful(agentAllocator.allocate(provisionConfig, machines))
      .flatMapToAdt(x => spawnComponentsByMapping(x), identity)

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
      .spawnSequenceComponent(seqCompPrefix.componentName)
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

}
