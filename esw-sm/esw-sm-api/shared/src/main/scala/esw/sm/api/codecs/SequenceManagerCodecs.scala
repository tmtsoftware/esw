package esw.sm.api.codecs

import csw.location.api.codec.LocationCodecs
import esw.ocs.api.codecs.OcsCodecs
import esw.sm.api.models.{AgentProvisionConfig, ProvisionConfig, _}
import esw.sm.api.protocol.{ObsModesDetailsResponse, _}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs
import io.bullet.borer.derivation.MapBasedCodecs.{deriveAllCodecs, deriveCodec}
import msocket.api.codecs.BasicCodecs

object SequenceManagerCodecs extends SequenceManagerCodecs

trait SequenceManagerCodecs extends LocationCodecs with BasicCodecs with OcsCodecs {
  implicit lazy val agentProvisionConfigCodec: Codec[AgentProvisionConfig] = deriveCodec
  implicit lazy val provisionConfigCodec: Codec[ProvisionConfig]           = deriveCodec

  implicit lazy val configureResponseCodec: Codec[ConfigureResponse]                                 = deriveAllCodecs
  implicit lazy val resourcesCodec: Codec[Resources]                                                 = CompactMapBasedCodecs.deriveCodec
  implicit lazy val sequencersCodec: Codec[Sequencers]                                               = CompactMapBasedCodecs.deriveCodec
  implicit lazy val obsModeStatusCodec: Codec[ObsModeStatus]                                         = deriveAllCodecs
  implicit lazy val getObsModeDetailsCodec: Codec[ObsModeDetails]                                    = deriveCodec
  implicit lazy val getObsModesDetailsResponseCodec: Codec[ObsModesDetailsResponse]                  = deriveAllCodecs
  implicit lazy val startSequencerResponseCodec: Codec[StartSequencerResponse]                       = deriveAllCodecs
  implicit lazy val shutdownSequenceComponentResponseCodec: Codec[ShutdownSequenceComponentResponse] = deriveAllCodecs
  implicit lazy val shutdownSequencersResponseCodec: Codec[ShutdownSequencersResponse]               = deriveAllCodecs
  implicit lazy val restartSequencerResponseCodec: Codec[RestartSequencerResponse]                   = deriveAllCodecs
  implicit lazy val provisionResponseCodec: Codec[ProvisionResponse]                                 = deriveAllCodecs

  implicit lazy val resourcesStatusResponseCodec: Codec[ResourcesStatusResponse] = deriveAllCodecs
  implicit lazy val resourceStatusResponseCodec: Codec[ResourceStatusResponse]   = deriveCodec
  implicit lazy val resourceStatusCodec: Codec[ResourceStatus]                   = deriveAllCodecs
  implicit lazy val resourceCodec: Codec[Resource]                               = CompactMapBasedCodecs.deriveCodec

}
