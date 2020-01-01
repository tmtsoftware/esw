package esw.ocs.api.codecs

import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.ParamCodecs
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.CompactMapBasedCodecs._
import msocket.api.codecs.BasicCodecs

object OcsCodecs extends OcsCodecs
trait OcsCodecs extends OcsCodecsBase {
  implicit def responseCodec[T <: EswSequencerResponse]: Codec[T] = responseCodecValue.asInstanceOf[Codec[T]]
}
trait OcsCodecsBase extends ParamCodecs with LocationCodecs with BasicCodecs {
  //StepList Codecs
  implicit lazy val stepCodec: Codec[Step]             = deriveCodec
  implicit lazy val stepListCodec: Codec[StepList]     = deriveCodec
  implicit lazy val stepStatusCodec: Codec[StepStatus] = deriveAllCodecs

  //EswSequencerResponse Codecs
  implicit lazy val okCodec: Codec[Ok.type]                                                               = deriveCodec
  implicit lazy val unhandledCodec: Codec[Unhandled]                                                      = deriveCodec
  implicit lazy val idDoesNotExistCodec: Codec[IdDoesNotExist]                                            = deriveCodec
  implicit lazy val inFlightOrFinishedStepErrorCodec: Codec[CannotOperateOnAnInFlightOrFinishedStep.type] = deriveCodec
  lazy val responseCodecValue: Codec[EswSequencerResponse]                                                = deriveAllCodecs

  //SequenceComponentResponse Codecs
  implicit lazy val loadScriptResponseCodec: Codec[ScriptResponse]   = deriveCodec
  implicit lazy val getStatusResponseCodec: Codec[GetStatusResponse] = deriveCodec

  implicit lazy val scriptErrorCodec: Codec[ScriptError] = deriveCodec
}
