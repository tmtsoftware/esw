package esw.agent.akka.client.models

import com.typesafe.config.Config

import java.nio.file.{Path, Paths}

case class ContainerConfig(
    orgName: String,
    deployModule: String,
    appName: String,
    version: String,
    mode: String,
    configFilePath: Path,
    isConfigLocal: Boolean
)

object ContainerConfig {
  def apply(config: Config): ContainerConfig = {
    ContainerConfig(
      config.getString("orgName"),
      config.getString("deployModule"),
      config.getString("appName"),
      config.getString("version"),
      config.getString("mode"),
      Paths.get(config.getString("configFilePath")),
      config.getString("configFileLocation") == "Local"
    )
  }
}
