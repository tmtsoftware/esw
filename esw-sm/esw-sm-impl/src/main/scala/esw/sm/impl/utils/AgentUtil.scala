package esw.sm.impl.utils

import akka.actor.typed.{ActorSystem, Scheduler}
import csw.location.api.models.ComponentType.{Machine, SequenceComponent}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Prefix
import esw.agent.api._
import esw.agent.client.AgentClient
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.SpawnSequenceComponentResponse.{SpawnSequenceComponentFailed, Success}
import esw.sm.api.protocol.{ProvisionResponse, SpawnSequenceComponentResponse}
import esw.sm.impl.config.ProvisionConfig

import scala.concurrent.Future

class AgentUtil(locationServiceUtil: LocationServiceUtil)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  // todo: remove the logic to resolving seq comp after spawning
  def spawnSequenceComponentOn(
      machine: Prefix,
      seqCompName: String
  ): Future[SpawnSequenceComponentResponse] =
    getAgent(machine).flatMapToAdt(spawnSeqComp(_, Prefix(machine.subsystem, seqCompName)), identity)

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] =
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .mapLeft(e => LocationServiceError(e.msg))
      .flatMapE(provisionOn(_, provisionConfig))
      .mapToAdt(spawnResToProvisionRes, identity)

  def getSequenceComponentsRunningOn(agents: List[AkkaLocation]): Future[Map[ComponentId, List[ComponentId]]] =
    Future
      .traverse(agents) { agent =>
        makeAgentClient(agent).getAgentStatus
          .map(filterRunningSeqComps)
          .map(seqComps => agent.connection.componentId -> seqComps)
      }
      .map(_.toMap)

  private def provisionOn(
      machines: List[AkkaLocation],
      provisionConfig: ProvisionConfig
  ): Future[Either[ProvisionResponse.NoMachineFoundForSubsystems, List[SpawnResponse]]] =
    Future
      .successful(AgentAllocator(machines).allocate(provisionConfig))
      .flatMapRight(x => spawnComponentsByMapping(x.flatMap(_.toList)))

  private def spawnComponentsByMapping(mappings: List[(Prefix, AkkaLocation)]) =
    Future.traverse(mappings) { seqCompMapping =>
      val (prefix, machine) = seqCompMapping
      makeAgentClient(machine).spawnSequenceComponent(prefix)
    }

  private def spawnResToProvisionRes(responses: List[SpawnResponse]): ProvisionResponse = {
    val failedResponses = responses.collect { case Failed(msg) => SpawnSequenceComponentFailed(msg) }

    if (failedResponses.isEmpty) ProvisionResponse.Success
    else ProvisionResponse.SpawningSequenceComponentsFailed(failedResponses)
  }

  private def spawnSeqComp(agentClient: AgentClient, seqCompPrefix: Prefix) =
    agentClient
      .spawnSequenceComponent(seqCompPrefix)
      .flatMap {
        case Spawned     => Future.successful(Success(ComponentId(seqCompPrefix, SequenceComponent)))
        case Failed(msg) => Future.successful(SpawnSequenceComponentFailed(msg))
      }

  private[utils] def getAgent(prefix: Prefix): Future[Either[LocationServiceError, AgentClient]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(prefix, Machine)))
      .mapRight(location => makeAgentClient(location))
      .mapLeft(error => LocationServiceError(error.msg))

  private[utils] def makeAgentClient(loc: AkkaLocation): AgentClient = {
    implicit val sch: Scheduler = actorSystem.scheduler
    new AgentClient(loc)
  }

  private def filterRunningSeqComps(agentStatus: AgentStatus): List[ComponentId] =
    agentStatus.componentStatus
      .filter {
        case (componentId, componentStatus) =>
          componentId.componentType == SequenceComponent && componentStatus == ComponentStatus.Running
      }
      .keys
      .toList
}
