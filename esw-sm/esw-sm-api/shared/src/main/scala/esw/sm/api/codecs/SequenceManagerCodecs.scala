package esw.sm.api.codecs

import csw.location.api.codec.LocationCodecs
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse, StartSequencerResponse}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

object SequenceManagerCodecs extends SequenceManagerCodecs

trait SequenceManagerCodecs extends LocationCodecs {
  implicit lazy val configureResponseCodec: Codec[ConfigureResponse]                   = deriveAllCodecs
  implicit lazy val getRunningObsModesResponseCodec: Codec[GetRunningObsModesResponse] = deriveAllCodecs
  implicit lazy val cleanupResponseCodec: Codec[CleanupResponse]                       = deriveAllCodecs
  implicit lazy val startSequencerResponseCodec: Codec[StartSequencerResponse]         = deriveAllCodecs
}
