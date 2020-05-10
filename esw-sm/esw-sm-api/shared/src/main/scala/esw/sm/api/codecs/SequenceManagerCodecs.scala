package esw.sm.api.codecs

import csw.location.api.codec.LocationCodecs
import esw.sm.api.models._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

object SequenceManagerCodecs extends SequenceManagerCodecs

trait SequenceManagerCodecs extends LocationCodecs {
  // Codecs for SequenceManagerConfig to parse config json string to domain model using borer
  implicit lazy val obsModeConfigCodec: Codec[ObsModeConfig]                 = deriveCodec
  implicit lazy val resourcesCodec: Codec[Resources]                         = deriveCodec
  implicit lazy val sequencersCodec: Codec[Sequencers]                       = deriveCodec
  implicit lazy val sequenceManagerConfigCodec: Codec[SequenceManagerConfig] = deriveCodec

  // SequenceManagerResponse Codecs
  implicit lazy val configureResponseCodec: Codec[ConfigureResponse]                   = deriveAllCodecs
  implicit lazy val getRunningObsModesResponseCodec: Codec[GetRunningObsModesResponse] = deriveAllCodecs
  implicit lazy val cleanupResponseCodec: Codec[CleanupResponse]                       = deriveAllCodecs
}
