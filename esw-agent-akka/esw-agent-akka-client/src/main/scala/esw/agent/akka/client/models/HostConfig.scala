package esw.agent.akka.client.models

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.location.api.models.ComponentType
import csw.prefix.models.Prefix
import esw.agent.akka.client.codecs.AgentActorCodecs
import io.bullet.borer.Json

import java.nio.file.Path

/**
 * This is a representation of a single Container. It is collectively being used in HostConfig.
 * For e.g.
 * {
    orgName: "com.github.tmtsoftware.sample"
    deployModule: "csw-sampledeploy"
    appName: "csw.sampledeploy.SampleContainerCmdApp"
    version: "0.0.1"
    mode: "Container"
    configFilePath: "confPath1.conf"
    configFileLocation: "Local"
  }
 * @param orgName - name of the organization. Ideally this would be same as github repo name/ package name.
 * @param deployModule - name of the module which needs to be started.
 * @param appName - complete reference name of the Application.
 * @param version -
 * @param mode [[ContainerMode]] - mode in which Application needs to be started.
 * @param configFilePath [[java.lang.String]] - Path of the config file for the application.
 * @param configFileLocation [[ConfigFileLocation]] - Type of the config file location.
 */
case class ContainerConfig(
    orgName: String,
    deployModule: String,
    appName: String,
    version: String,
    mode: ContainerMode,
    configFilePath: Path,
    configFileLocation: ConfigFileLocation
)

object ContainerConfig extends AgentActorCodecs {
  def apply(config: Config): ContainerConfig =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[ContainerConfig].value
}

case class ContainerInfo(name: String)

object ContainerInfo extends AgentActorCodecs {
  def apply(config: Config): ContainerInfo =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[ContainerInfo].value
}

/**
 * Represents an identity to a container.
 * @param prefix [[csw.prefix.models.Prefix]] - unique prefix that will be used to register this container in location service.
 * @param componentType [[csw.location.api.models.ComponentType]] - type of container : HCD / Assembly / Container.
 */
case class ComponentInfo(prefix: Prefix, componentType: ComponentType)

object ComponentInfo extends AgentActorCodecs {
  def apply(config: Config): ComponentInfo =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[ComponentInfo].value
}

/**
 * This is a representation for the config file of a Agent.
 * @param containers - list of [[ContainerConfig]] that will be used to spawn containers by the agent while it is getting spawned.
 */
case class HostConfig(containers: List[ContainerConfig])

object HostConfig extends AgentActorCodecs {
  def apply(config: Config): HostConfig =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[HostConfig].value
}
