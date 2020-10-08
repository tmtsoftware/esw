package esw.agent.service.api

import java.nio.file.Path

import csw.location.api.models.Connection
import csw.prefix.models.Prefix
import esw.agent.service.api.models.{KillResponse, SpawnResponse}

import scala.concurrent.Future

trait AgentServiceApi {

  /**
   * spawn sequence manager on a specific agent
   * It will return [[esw.agent.service.api.models.Spawned]] if component is spawned successfully
   * otherwise it will return [[esw.agent.service.api.models.Failed]]
   *
   * @param agentPrefix - prefix of the agent
   * @param obsModeConfigPath - path of obsMode config file
   * @param isConfigLocal - is config local or not
   * @param version - binary version
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
   * @param version - binary version
   * @return [[esw.agent.service.api.models.SpawnResponse]] as a Future value
   */
  def spawnSequenceComponent(agentPrefix: Prefix, componentName: String, version: Option[String] = None): Future[SpawnResponse]

  /**
   * kill the component of given connection which was spawned by an agent
   * It will return [[esw.agent.service.api.models.Killed]] if component is killed successfully
   * otherwise it will return [[esw.agent.service.api.models.Failed]]
   *
   * @param connection of component
   * @return [[esw.agent.service.api.models.SpawnResponse]] as a Future value
   */
  def killComponent(connection: Connection): Future[KillResponse]
}
