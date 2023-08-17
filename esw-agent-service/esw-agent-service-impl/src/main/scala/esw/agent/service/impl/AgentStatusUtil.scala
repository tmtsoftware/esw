package esw.agent.service.impl

import org.apache.pekko.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.{PekkoLocation, ComponentId}
import esw.agent.service.api.models.AgentStatusResponse.LocationServiceError
import esw.agent.service.api.models.{AgentStatus, AgentStatusResponse, SequenceComponentStatus}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.MapExt.MapOps
import esw.commons.utils.location.LocationServiceUtil

import scala.concurrent.Future

/**
 * A utility class written using locationServiceUtil to be used in AgentServiceImpl
 * @param locationServiceUtil - an instance of locationServiceUtil
 * @param actorSystem - an implicit actor system
 */
class AgentStatusUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  // returns map of all agents with information about sequence components & sequencer running on it.
  // agentId is collected using metadata field of sequenceComponent/sequencer registered location.
  // In case when agentId is not found in the metadata, sequenceComponent/sequencer are collected in a separate list named orphans.
  def getAllAgentStatus: Future[AgentStatusResponse] =
    getAllSequenceComponents
      .mapRight(groupByAgentWithOrphans)
      .flatMapE { case (agentMap, compsWithoutAgent) =>
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

  // returns map of agent and seq comps running on agent as well as list of seq comps without agent information available
  private def groupByAgentWithOrphans(seqComps: List[PekkoLocation]) = {
    val (agentMap, orphans) = seqComps.partitionMap { loc =>
      loc.metadata.getAgentPrefix match {
        case Some(agentPrefix) => Left(ComponentId(agentPrefix, Machine) -> loc)
        case None              => Right(loc)
      }
    }
    (agentMap.groupMap(_._1)(_._2), orphans)
  }

  private def getAndAddAgentsWithoutSeqComp(agents: Map[ComponentId, List[PekkoLocation]]) =
    getAllAgentIds.mapRight(agents.addKeysIfNotExist(_, List.empty))
  private def getAllAgents             = locationServiceUtil.listPekkoLocationsBy(Machine)
  private def getAllAgentIds           = getAllAgents.mapRight(_.map(_.connection.componentId))
  private def getAllSequenceComponents = locationServiceUtil.listPekkoLocationsBy(SequenceComponent)
  private def getAllSequencers         = locationServiceUtil.listPekkoLocationsBy(Sequencer)

  private def getAgentStatus(
      agents: Map[ComponentId, List[PekkoLocation]],
      sequencers: List[PekkoLocation]
  ): List[AgentStatus] =
    agents.toList.map { case (agentId, seqCompLocations) =>
      AgentStatus(agentId, getSeqCompsStatus(seqCompLocations, sequencers))
    }

  private def getSeqCompsStatus(
      seqComps: List[PekkoLocation],
      sequencers: List[PekkoLocation]
  ): List[SequenceComponentStatus] =
    seqComps.map(seqComp =>
      SequenceComponentStatus(
        seqComp.connection.componentId,
        sequencers.find(_.metadata.getSequenceComponentPrefix.contains(seqComp.prefix))
      )
    )
}
