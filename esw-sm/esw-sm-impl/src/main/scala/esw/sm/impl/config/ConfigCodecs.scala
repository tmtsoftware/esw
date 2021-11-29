package esw.sm.impl.config

import csw.prefix.codecs.CommonCodecs
import esw.ocs.api.codecs.OcsCodecs
import esw.sm.api.models.{Resource, Resources, SequencerIds}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec

object ConfigCodecs extends CommonCodecs with OcsCodecs {
  // Codecs for SequenceManagerConfig to parse config json string to domain model using borer

  implicit lazy val obsModeConfigCodec: Codec[ObsModeConfig]                 = deriveCodec
  implicit lazy val resourceCodec: Codec[Resource]                           = deriveCodec
  implicit lazy val resourcesCodec: Codec[Resources]                         = deriveCodec
  implicit lazy val sequencersCodec: Codec[SequencerIds]                     = deriveCodec
  implicit lazy val sequenceManagerConfigCodec: Codec[SequenceManagerConfig] = deriveCodec

}
