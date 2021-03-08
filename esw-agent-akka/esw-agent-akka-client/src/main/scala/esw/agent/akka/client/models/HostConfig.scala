package esw.agent.akka.client.models

import com.typesafe.config.{Config, ConfigRenderOptions}
import csw.location.api.models.ComponentType
import csw.prefix.models.Prefix
import esw.agent.akka.client.codecs.AgentActorCodecs
import io.bullet.borer.Json

import java.nio.file.Path

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

case class ComponentInfo(prefix: Prefix, componentType: ComponentType)

object ComponentInfo extends AgentActorCodecs {
  def apply(config: Config): ComponentInfo =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[ComponentInfo].value
}

case class HostConfig(containers: List[ContainerConfig])

object HostConfig extends AgentActorCodecs {
  def apply(config: Config): HostConfig =
    Json.decode(config.root().render(ConfigRenderOptions.concise()).getBytes()).to[HostConfig].value
}
