package esw.agent.service.api

import java.nio.file.Path

import csw.location.api.models.ComponentId
import csw.prefix.models.Prefix
import esw.agent.service.api.models.{AgentStatusResponse, KillResponse, SpawnContainersResponse, SpawnResponse}

import scala.concurrent.Future

/**
 * A Agent Service API for interacting with Agent of specific machine's.
 */
trait AgentServiceApi {

  /**
   * spawn sequence manager on a specific agent
   * It will return [[esw.agent.service.api.models.Spawned]] if component is spawned successfully
   * otherwise it will return [[esw.agent.service.api.models.Failed]]
   *
   * @param agentPrefix - prefix of the agent
   * @param obsModeConfigPath - path of obsMode config file
   * @param isConfigLocal - is config local or not
   * @param version - binary version of sequence-manager
   * @return [[esw.agent.service.api.models.SpawnResponse]] as a Future value
   */
  def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String] = None
  ): Future[SpawnResponse]

  /**
   * spawn sequence component on a specific agent
   * It will return [[esw.agent.service.api.models.Spawned]] if component is spawned successfully
   * otherwise it will return [[esw.agent.service.api.models.Failed]]
   *
   * @param agentPrefix - prefix of the agent
   * @param componentName - name of the sequence component
   * @param version - binary version of ocs-app
   * @return [[esw.agent.service.api.models.SpawnResponse]] as a Future value
   */
  def spawnSequenceComponent(agentPrefix: Prefix, componentName: String, version: Option[String] = None): Future[SpawnResponse]

  /**
   * spawn containers using a specific agent
   * It will return [[esw.agent.service.api.models.SpawnContainersResponse]] indicating the status of spawned containers
   *
   * @param agentPrefix - prefix of the agent
   * @param hostConfigPath - path of host config file
   * @param isConfigLocal - true if host config is to be read from local filesystem
   * @return [[esw.agent.service.api.models.SpawnContainersResponse]] as a Future value
   */
  def spawnContainers(agentPrefix: Prefix, hostConfigPath: String, isConfigLocal: Boolean): Future[SpawnContainersResponse]

  /**
   * kill the component of given componentId which was spawned by an agent
   * It will return [[esw.agent.service.api.models.Killed]] if component is killed successfully
   * otherwise it will return [[esw.agent.service.api.models.Failed]]
   *
   * @param componentId of component
   * @return [[esw.agent.service.api.models.SpawnResponse]] as a Future value
   */
  def killComponent(componentId: ComponentId): Future[KillResponse]

  /**
   * Gives information of all running agents, sequence components running on those agents as well as orphan sequence
   * components (agent unknown) and sequencers running on sequence components.
   * @return a future of [[esw.agent.service.api.models.AgentStatusResponse]] which completes with Success or Failure response ADT
   */
  def getAgentStatus: Future[AgentStatusResponse]
}
