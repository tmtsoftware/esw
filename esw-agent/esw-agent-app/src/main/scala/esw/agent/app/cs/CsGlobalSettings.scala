package esw.agent.app.cs

import com.typesafe.config.{Config, ConfigFactory, ConfigRenderOptions}
import esw.agent.app.cs.CsSettings.OcsAppSettings
import io.bullet.borer.derivation.MapBasedCodecs._
import io.bullet.borer.{Codec, Json}

case class CsGlobalSettings(channel: String, ocsApp: OcsAppSettings)

object CsGlobalSettings {
  implicit lazy val csGlobalSettingsCodec: Codec[CsGlobalSettings] = deriveCodec

  def apply(config: Config = ConfigFactory.load()): CsGlobalSettings = {
    val agentConfig = config.getConfig("agent.cs")
    val configStr   = agentConfig.root().render(ConfigRenderOptions.concise())

    Json.decode(configStr.getBytes).to[CsGlobalSettings].value
  }
}
