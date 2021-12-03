package esw.ocs.api.codecs

import csw.location.api.codec.LocationCodecs
import csw.params.core.formats.ParamCodecs
import esw.ocs.api.models.*
import esw.ocs.api.protocol.*
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, OkOrUnhandled, ScriptResponseOrUnhandled}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs.deriveCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs
import msocket.api.codecs.BasicCodecs

/**
 * Codecs for the models which are being used in protocol(as a response/request model) communication(actor/http)
 */
object OcsCodecs extends OcsCodecs
trait OcsCodecs extends OcsCodecsBase {
  implicit def responseCodec[T <: EswSequencerResponse]: Codec[T] = responseCodecValue.asInstanceOf[Codec[T]]
}
trait OcsCodecsBase extends ParamCodecs with LocationCodecs with BasicCodecs {
  //StepList Codecs
  implicit lazy val stepCodec: Codec[Step]             = deriveCodec
  implicit lazy val stepListCodec: Codec[StepList]     = deriveCodec
  implicit lazy val stepStatusCodec: Codec[StepStatus] = deriveAllCodecs

  //ObsMode Codecs
  implicit lazy val obsModeCodec: Codec[ObsMode]               = deriveCodec
  implicit lazy val sequencerStateCodec: Codec[SequencerState] = deriveAllCodecs

  implicit lazy val getStatusResponseCodec: Codec[GetStatusResponse] = deriveCodec

  //EswSequencerResponse Codecs
  lazy val responseCodecValue: Codec[EswSequencerResponse] = deriveAllCodecs

  //SequenceComponentResponse Codecs
  implicit lazy val loadScriptResponseCodec: Codec[ScriptResponseOrUnhandled] = deriveAllCodecs
  implicit lazy val okOrUnhandledCodec: Codec[OkOrUnhandled]                  = deriveAllCodecs

  implicit lazy val scriptErrorCodec: Codec[ScriptError] = deriveAllCodecs

  implicit lazy val variationCodec: Codec[Variation]     = deriveCodec
  implicit lazy val variationIdCodec: Codec[VariationId] = Codec.bimap[String, VariationId](_.toString, VariationId.from)
}
