package esw.sm.api.codecs

import csw.prefix.codecs.CommonCodecs
import esw.sm.api.models.{ObsModeConfig, Resources, SequenceManagerConfig, Sequencers}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

trait SequenceManagerCodecs extends CommonCodecs {
  implicit lazy val obsModeConfigCodec: Codec[ObsModeConfig]                 = deriveCodec
  implicit lazy val resourcesCodec: Codec[Resources]                         = deriveCodec
  implicit lazy val sequencersCodec: Codec[Sequencers]                       = deriveCodec
  implicit lazy val sequenceManagerConfigCodec: Codec[SequenceManagerConfig] = deriveCodec
}
