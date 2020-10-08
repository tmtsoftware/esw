package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, Spawned}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.extensions.MapExt.MapOps
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.models.{AgentStatus, ProvisionConfig, SequenceComponentStatus}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse.SpawningSequenceComponentsFailed
import esw.sm.api.protocol.{AgentStatusResponse, ProvisionResponse}
import esw.sm.impl.utils.Types._

import scala.concurrent.Future

class AgentUtil(
    locationServiceUtil: LocationServiceUtil,
    sequenceComponentUtil: SequenceComponentUtil,
    agentAllocator: AgentAllocator
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def getAllAgentStatus: Future[AgentStatusResponse] =
    getAllSequenceComponents
      .mapRight(groupByAgentWithOrphans)
      .flatMapE {
        case (agentMap, compsWithoutAgent) =>
          getAndAddAgentsWithoutSeqComp(agentMap).flatMapRight(
            getAgentStatus(_) zip getSeqCompsStatus(compsWithoutAgent)
          )
      }
      .mapToAdt(
        { case (agentStatus, orphans) => AgentStatusResponse.Success(agentStatus, orphans) },
        e => LocationServiceError(e.msg)
      )

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] =
    getAllAgents
      .mapLeft(e => LocationServiceError(e.msg))
      .flatMapToAdt(provisionOn(_, provisionConfig), identity)

  // returns map of agent and seq comps running on agent as well as list of seq comps without agent information available
  private def groupByAgentWithOrphans(seqComps: List[SeqCompLocation]) = {
    val (agentMap, orphans) = seqComps.partitionMap { loc =>
      loc.metadata.getAgentPrefix match {
        case Some(agentPrefix) => Left(ComponentId(agentPrefix, Machine) -> loc)
        case None              => Right(loc)
      }
    }
    (agentMap.groupMap(_._1)(_._2), orphans)
  }

  private def getAndAddAgentsWithoutSeqComp(agents: Map[AgentId, List[SeqCompLocation]]) =
    getAllAgentIds.mapRight(agents.addKeysIfNotExist(_, List.empty))
  private def getAllAgents             = locationServiceUtil.listAkkaLocationsBy(Machine)
  private def getAllAgentIds           = getAllAgents.mapRight(_.map(_.connection.componentId))
  private def getAllSequenceComponents = locationServiceUtil.listAkkaLocationsBy(SequenceComponent)

  private def getAgentStatus(agents: Map[AgentId, List[SeqCompLocation]]): Future[List[AgentStatus]] =
    Future.traverse(agents.toList) {
      case (agentId, seqCompLocations) => getSeqCompsStatus(seqCompLocations).map(AgentStatus(agentId, _))
    }

  private def getSeqCompsStatus(seqComps: List[SeqCompLocation]): Future[List[SequenceComponentStatus]] =
    Future.traverse(seqComps)(sequenceComponentUtil.getSequenceComponentStatus)

  private def provisionOn(agentLocations: List[AgentLocation], provisionConfig: ProvisionConfig) =
    Future
      .successful(agentAllocator.allocate(provisionConfig, agentLocations))
      .flatMapToAdt(spawnCompsByMapping, identity)

  private def spawnCompsByMapping(mapping: List[(AgentLocation, SeqCompPrefix)]): Future[ProvisionResponse] =
    Future
      .traverse(mapping) {
        case (agentLocation, seqCompPrefix) => spawnSeqComp(agentLocation.prefix, makeAgentClient(agentLocation), seqCompPrefix)
      }
      .map(_.sequence.map(_ => ProvisionResponse.Success).left.map(SpawningSequenceComponentsFailed).merge)

  private def spawnSeqComp(agentPrefix: AgentPrefix, agentClient: AgentClient, seqCompPrefix: SeqCompPrefix) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix.componentName)
      .map {
        case Spawned     => Right(())
        case Failed(msg) => Left(s"Failed to spawn Sequence component: $seqCompPrefix on Machine: $agentPrefix, reason: $msg")
      }

  private[utils] def getAndMakeAgentClient(agentPrefix: AgentPrefix): Future[Either[LocationServiceError, AgentClient]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(agentPrefix, Machine)))
      .mapRight(location => makeAgentClient(location))
      .mapLeft(error => LocationServiceError(error.msg))

  private[utils] def makeAgentClient(agentLocation: AgentLocation): AgentClient = new AgentClient(agentLocation)
}
