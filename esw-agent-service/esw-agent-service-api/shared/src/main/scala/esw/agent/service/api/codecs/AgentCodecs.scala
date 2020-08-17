package esw.agent.service.api.codecs

import java.nio.file.{Path, Paths}

import csw.location.api.codec.LocationCodecs
import esw.agent.service.api.models.{AgentResponse, KillResponse, SpawnResponse}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

trait AgentCodecs extends LocationCodecs {
  implicit lazy val pathCodec: Codec[Path]                   = Codec.bimap[String, Path](_.toString, Paths.get(_))
  implicit lazy val agentResponseCodec: Codec[AgentResponse] = deriveAllCodecs
  implicit lazy val spawnResponseCodec: Codec[SpawnResponse] = deriveAllCodecs
  implicit lazy val killResponseCodec: Codec[KillResponse]   = deriveAllCodecs
}
