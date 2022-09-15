/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.service.impl

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.{AkkaLocation, ComponentId}
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
  private def groupByAgentWithOrphans(seqComps: List[AkkaLocation]) = {
    val (agentMap, orphans) = seqComps.partitionMap { loc =>
      loc.metadata.getAgentPrefix match {
        case Some(agentPrefix) => Left(ComponentId(agentPrefix, Machine) -> loc)
        case None              => Right(loc)
      }
    }
    (agentMap.groupMap(_._1)(_._2), orphans)
  }

  private def getAndAddAgentsWithoutSeqComp(agents: Map[ComponentId, List[AkkaLocation]]) =
    getAllAgentIds.mapRight(agents.addKeysIfNotExist(_, List.empty))
  private def getAllAgents             = locationServiceUtil.listAkkaLocationsBy(Machine)
  private def getAllAgentIds           = getAllAgents.mapRight(_.map(_.connection.componentId))
  private def getAllSequenceComponents = locationServiceUtil.listAkkaLocationsBy(SequenceComponent)
  private def getAllSequencers         = locationServiceUtil.listAkkaLocationsBy(Sequencer)

  private def getAgentStatus(
      agents: Map[ComponentId, List[AkkaLocation]],
      sequencers: List[AkkaLocation]
  ): List[AgentStatus] =
    agents.toList.map { case (agentId, seqCompLocations) =>
      AgentStatus(agentId, getSeqCompsStatus(seqCompLocations, sequencers))
    }

  private def getSeqCompsStatus(
      seqComps: List[AkkaLocation],
      sequencers: List[AkkaLocation]
  ): List[SequenceComponentStatus] =
    seqComps.map(seqComp =>
      SequenceComponentStatus(
        seqComp.connection.componentId,
        sequencers.find(_.metadata.getSequenceComponentPrefix.contains(seqComp.prefix))
      )
    )
}
