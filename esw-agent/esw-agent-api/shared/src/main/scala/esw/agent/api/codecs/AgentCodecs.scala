package esw.agent.api.codecs

import java.nio.file.{Path, Paths}

import csw.location.api.codec.LocationCodecs
import esw.agent.api.{ComponentStatus, KillResponse, Response, SpawnResponse}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait AgentCodecs extends LocationCodecs {
  implicit lazy val pathCodec: Codec[Path]                       = Codec.bimap[String, Path](_.toString, Paths.get(_))
  implicit lazy val componentStatusCodec: Codec[ComponentStatus] = deriveAllCodecs
  implicit lazy val agentResponseCodec: Codec[Response]          = deriveAllCodecs
  implicit lazy val spawnResponseCodec: Codec[SpawnResponse]     = deriveAllCodecs
  implicit lazy val killResponseCodec: Codec[KillResponse]       = deriveAllCodecs
}
