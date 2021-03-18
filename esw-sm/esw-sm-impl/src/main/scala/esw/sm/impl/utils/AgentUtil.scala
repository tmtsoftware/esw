package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, Spawned}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.extensions.MapExt.MapOps
import esw.commons.utils.config.{FetchingScriptVersionFailed, VersionManager}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.models.{AgentStatus, ProvisionConfig, SequenceComponentStatus}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse.{ProvisionVersionFailure, SpawningSequenceComponentsFailed}
import esw.sm.api.protocol.{AgentStatusResponse, ProvisionResponse}
import esw.sm.impl.utils.Types._

import scala.concurrent.Future

class AgentUtil(
    locationServiceUtil: LocationServiceUtil,
    agentAllocator: AgentAllocator,
    versionManager: VersionManager,
    simulation: Boolean = false
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def getAllAgentStatus: Future[AgentStatusResponse] =
    getAllSequenceComponents
      .mapRight(groupByAgentWithOrphans)
      .flatMapE {
        case (agentMap, compsWithoutAgent) =>
          getAndAddAgentsWithoutSeqComp(agentMap).flatMapE { agentMap =>
            getAllSequencers.mapRight { sequencers =>
              (getAgentStatus(agentMap, sequencers), getSeqCompsStatus(compsWithoutAgent, sequencers))
            }
          }
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
  private def getAllSequencers         = locationServiceUtil.listAkkaLocationsBy(Sequencer)

  private def getAgentStatus(
      agents: Map[AgentId, List[SeqCompLocation]],
      sequencers: List[SequencerLocation]
  ): List[AgentStatus] =
    agents.toList.map {
      case (agentId, seqCompLocations) => AgentStatus(agentId, getSeqCompsStatus(seqCompLocations, sequencers))
    }

  private def getSeqCompsStatus(
      seqComps: List[SeqCompLocation],
      sequencers: List[SequencerLocation]
  ): List[SequenceComponentStatus] =
    seqComps.map(seqComp =>
      SequenceComponentStatus(
        seqComp.connection.componentId,
        sequencers.find(_.metadata.getSequenceComponentPrefix.contains(seqComp.prefix))
      )
    )

  private def provisionOn(agentLocations: List[AgentLocation], provisionConfig: ProvisionConfig) = {
    Future
      .successful(agentAllocator.allocate(provisionConfig, agentLocations))
      .flatMapToAdt(spawnSeqCompByVersion, identity)
  }

  private def spawnSeqCompByVersion(mapping: List[(AgentLocation, SeqCompPrefix)]) = {
    versionManager.getScriptVersion.flatMap(spawnCompsByMapping(mapping, _)).recover {
      case FetchingScriptVersionFailed(msg) => ProvisionVersionFailure(msg)
    }
  }

  private def spawnCompsByMapping(
      mapping: List[(AgentLocation, SeqCompPrefix)],
      version: String
  ): Future[ProvisionResponse] =
    Future
      .traverse(mapping) {
        case (agentLocation, seqCompPrefix) =>
          spawnSeqComp(agentLocation.prefix, makeAgentClient(agentLocation), seqCompPrefix, version)
      }
      .map(_.sequence.map(_ => ProvisionResponse.Success).left.map(SpawningSequenceComponentsFailed).merge)

  private def spawnSeqComp(
      agentPrefix: AgentPrefix,
      agentClient: AgentClient,
      seqCompPrefix: SeqCompPrefix,
      version: String
  ) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix.componentName, Some(version), simulation)
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
