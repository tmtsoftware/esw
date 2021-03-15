package esw.agent.service.api.codecs

import csw.location.api.codec.LocationCodecs
import esw.agent.service.api.models.{AgentResponse, KillResponse, SpawnContainersResponse, SpawnResponse}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

import java.nio.file.{Path, Paths}

trait AgentCodecs extends LocationCodecs {
  implicit lazy val pathCodec: Codec[Path]                                       = Codec.bimap[String, Path](_.toString, Paths.get(_))
  implicit lazy val agentResponseCodec: Codec[AgentResponse]                     = deriveAllCodecs
  implicit lazy val spawnResponseCodec: Codec[SpawnResponse]                     = deriveAllCodecs
  implicit lazy val spawnContainersResponseCodec: Codec[SpawnContainersResponse] = deriveAllCodecs
  implicit lazy val killResponseCodec: Codec[KillResponse]                       = deriveAllCodecs
}
