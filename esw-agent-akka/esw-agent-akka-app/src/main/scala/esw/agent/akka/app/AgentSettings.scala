/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.agent.akka.app

import java.nio.file.Path

import com.typesafe.config.Config
import csw.prefix.models.Prefix

/**
 * @param prefix [[csw.prefix.models.Prefix]] - A unique identifier for the Agent
 * @param coursierChannel [[java.lang.String]] - Channel path to be used while spawning processes via Coursier
 * @param versionConfPath [[java.nio.file.Path]] - Path pointing to the location of version conf file. It can be absolute path or path inside configuration service.
 */
case class AgentSettings(
    prefix: Prefix,
    coursierChannel: String,
    versionConfPath: Path
)

object AgentSettings {
  def apply(prefix: Prefix, config: Config): AgentSettings = {
    val agentConfig = config.getConfig("agent")
    AgentSettings(
      prefix,
      agentConfig.getString("coursier.channel"),
      Path.of(agentConfig.getString("osw.version.confPath"))
    )
  }
}
