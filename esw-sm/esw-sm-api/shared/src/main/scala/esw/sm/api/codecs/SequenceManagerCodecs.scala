package esw.sm.api.codecs

import csw.location.api.codec.LocationCodecs
import esw.ocs.api.codecs.OcsCodecs
import esw.sm.api.protocol.AgentStatusResponses.{AgentSeqCompsStatus, SequenceComponentStatus}
import esw.sm.api.protocol._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import msocket.api.codecs.BasicCodecs

object SequenceManagerCodecs extends SequenceManagerCodecs

trait SequenceManagerCodecs extends LocationCodecs with BasicCodecs with OcsCodecs {
  implicit lazy val configureResponseCodec: Codec[ConfigureResponse]                                 = deriveAllCodecs
  implicit lazy val getRunningObsModesResponseCodec: Codec[GetRunningObsModesResponse]               = deriveAllCodecs
  implicit lazy val startSequencerResponseCodec: Codec[StartSequencerResponse]                       = deriveAllCodecs
  implicit lazy val spawnSequenceComponentResponseCodec: Codec[SpawnSequenceComponentResponse]       = deriveAllCodecs
  implicit lazy val shutdownSequenceComponentResponseCodec: Codec[ShutdownSequenceComponentResponse] = deriveAllCodecs
  implicit lazy val shutdownAllSequencersResponseCodec: Codec[ShutdownSequencersResponse]            = deriveAllCodecs
  implicit lazy val restartSequencerResponseCodec: Codec[RestartSequencerResponse]                   = deriveAllCodecs
  implicit lazy val SequenceComponentStatusCodec: Codec[SequenceComponentStatus]                     = deriveCodec
  implicit lazy val AgentStatusCodec: Codec[AgentSeqCompsStatus]                                     = deriveCodec
  implicit lazy val AgentStatusResponseCodec: Codec[AgentStatusResponse]                             = deriveAllCodecs
  implicit lazy val provisionResponseCodec: Codec[ProvisionResponse]                                 = deriveAllCodecs
}
