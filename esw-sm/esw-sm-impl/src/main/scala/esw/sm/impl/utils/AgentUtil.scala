package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Machine
import csw.location.api.models.Connection.AkkaConnection
import esw.agent.akka.client.AgentClient
import esw.agent.service.api.models.{Failed, Spawned}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.config.{FetchingScriptVersionFailed, VersionManager}
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.models.ProvisionConfig
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ProvisionResponse
import esw.sm.api.protocol.ProvisionResponse.{ProvisionVersionFailure, SpawningSequenceComponentsFailed}
import esw.sm.impl.utils.Types.*

import scala.concurrent.Future

class AgentUtil(
    locationServiceUtil: LocationServiceUtil,
    agentAllocator: AgentAllocator,
    versionManager: VersionManager,
    simulation: Boolean = false
)(implicit actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def provision(provisionConfig: ProvisionConfig): Future[ProvisionResponse] =
    getAllAgents
      .mapLeft(e => LocationServiceError(e.msg))
      .flatMapToAdt(provisionOn(_, provisionConfig), identity)

  // gets all the running agent from location service
  private def getAllAgents = locationServiceUtil.listAkkaLocationsBy(Machine)

  /*
   * provisions/starts all the sequence component on the required agent as mentioned in provision config
   *
   * if the agent is available within the given list of agents(agentLocations) - Success response is returned,
   * if unable to find the agent in the list - CouldNotFindMachines response is returned with the agent prefix,
   * if an error occurred while spawning the component - SpawningSequenceComponentsFailed response is returned
   * and if fails to fetch the sequencer-script repo with the provision version present in provision conf - ProvisionVersionFailure response is returned
   */
  private def provisionOn(agentLocations: List[AgentLocation], provisionConfig: ProvisionConfig) = {
    Future
      .successful(agentAllocator.allocate(provisionConfig, agentLocations))
      .flatMapToAdt(spawnSeqCompByVersion, identity)
  }

  /*
   * Goes through the list of (Agent, SequenceComponent) and starts each sequence component at required agent
   *
   * First it fetches the version from version config present in config service
   * and then it tell each agent to start sequence component's with that specific version(by the fetching the sequencer-script binary with that version)
   */
  private def spawnSeqCompByVersion(mapping: List[(AgentLocation, SeqCompPrefix)]) = {
    versionManager.getScriptVersion.flatMap(spawnCompsByMapping(mapping, _)).recover { case FetchingScriptVersionFailed(msg) =>
      ProvisionVersionFailure(msg)
    }
  }

  private def spawnCompsByMapping(
      mapping: List[(AgentLocation, SeqCompPrefix)],
      version: String
  ): Future[ProvisionResponse] =
    Future
      .traverse(mapping) { case (agentLocation, seqCompPrefix) =>
        spawnSeqComp(agentLocation.prefix, makeAgentClient(agentLocation), seqCompPrefix, version)
      }
      .map(_.sequence.map(_ => ProvisionResponse.Success).left.map(SpawningSequenceComponentsFailed).merge)

  /*
   * Spawn a sequence component with the given prefix on the agent of given agentPrefix.
   * It uses the agentClient given to it to spawn the sequence component.
   */
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

  /*
   * Creates an actor client for the agent of the given prefix
   */
  private[utils] def getAndMakeAgentClient(agentPrefix: AgentPrefix): Future[Either[LocationServiceError, AgentClient]] =
    locationServiceUtil
      .find(AkkaConnection(ComponentId(agentPrefix, Machine)))
      .mapRight(location => makeAgentClient(location))
      .mapLeft(error => LocationServiceError(error.msg))

  private[utils] def makeAgentClient(agentLocation: AgentLocation): AgentClient = new AgentClient(agentLocation)
}
