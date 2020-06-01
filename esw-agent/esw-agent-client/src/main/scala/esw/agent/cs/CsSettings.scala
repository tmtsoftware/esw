package esw.agent.cs

import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

sealed trait CsSettings {
  def name: String
  def version: String
  def javaOpts: List[String]
}

object CsSettings {
  case class OcsAppSettings(name: String, version: String, javaOpts: List[String]) extends CsSettings

  object OcsAppSettings {
    implicit lazy val ocsAppSettingsCodec: Codec[OcsAppSettings] = deriveCodec
  }
}
