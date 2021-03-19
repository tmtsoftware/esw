package esw.agent.service.api.codecs

import java.nio.file.{Path, Paths}

import csw.location.api.codec.LocationCodecs
import esw.agent.service.api.models._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}

trait AgentCodecs extends LocationCodecs {
  implicit lazy val pathCodec: Codec[Path]                                       = Codec.bimap[String, Path](_.toString, Paths.get(_))
  implicit lazy val agentResponseCodec: Codec[AgentResponse]                     = deriveAllCodecs
  implicit lazy val spawnResponseCodec: Codec[SpawnResponse]                     = deriveAllCodecs
  implicit lazy val spawnContainersResponseCodec: Codec[SpawnContainersResponse] = deriveAllCodecs
  implicit lazy val killResponseCodec: Codec[KillResponse]                       = deriveAllCodecs
  implicit lazy val SequenceComponentStatusCodec: Codec[SequenceComponentStatus] = deriveCodec
  implicit lazy val AgentStatusCodec: Codec[AgentStatus]                         = deriveCodec
  implicit lazy val agentStatusResponseCodec: Codec[AgentStatusResponse]         = deriveAllCodecs
}
