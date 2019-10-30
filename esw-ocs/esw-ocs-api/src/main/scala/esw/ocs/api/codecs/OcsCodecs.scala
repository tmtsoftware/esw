package esw.ocs.api.codecs

import csw.location.models.codecs.LocationCodecs
import csw.params.core.formats.ParamCodecs
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus.{InFlight, Pending}
import esw.ocs.api.models.{Step, StepList, StepStatus}
import esw.ocs.api.protocol.EditorError.{CannotOperateOnAnInFlightOrFinishedStep, IdDoesNotExist}
import esw.ocs.api.protocol._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec
import msocket.api.codecs.EitherCodecs

trait OcsCodecs extends ParamCodecs with LocationCodecs with EitherCodecs {
  //StepList Codecs
  implicit lazy val stepCodec: Codec[Step]                    = deriveCodec
  implicit lazy val stepListCodec: Codec[StepList]            = deriveCodec
  implicit lazy val successStatusCodec: Codec[Success]        = deriveCodec
  implicit lazy val failureStatusCodec: Codec[Failure]        = deriveCodec
  implicit lazy val pendingStatusCodec: Codec[Pending.type]   = deriveCodec
  implicit lazy val inflightStatusCodec: Codec[InFlight.type] = deriveCodec
  implicit lazy val stepStatusCodec: Codec[StepStatus]        = deriveCodec

  //StepListResponse Codecs
  implicit lazy val pullNextResultCodec: Codec[PullNextResult] = deriveCodec
  implicit lazy val sequenceResultCodec: Codec[SequenceResult] = deriveCodec
  implicit lazy val okCodec: Codec[Ok.type]                    = deriveCodec

  implicit lazy val unhandledCodec: Codec[Unhandled]                                                      = deriveCodec
  implicit lazy val idDoesNotExistCodec: Codec[IdDoesNotExist]                                            = deriveCodec
  implicit lazy val inFlightOrFinishedStepErrorCodec: Codec[CannotOperateOnAnInFlightOrFinishedStep.type] = deriveCodec
  implicit lazy val duplicateIdsFoundCodec: Codec[DuplicateIdsFound.type]                                 = deriveCodec
  implicit lazy val goOnlineHookFailedCodec: Codec[GoOnlineHookFailed.type]                               = deriveCodec
  implicit lazy val goOfflineHookFailedCodec: Codec[GoOfflineHookFailed.type]                             = deriveCodec
  implicit lazy val diagnosticHookFailedCodec: Codec[DiagnosticHookFailed.type]                           = deriveCodec
  implicit lazy val operationsHookFailedCodec: Codec[OperationsHookFailed.type]                           = deriveCodec
  implicit lazy val responseCodec: Codec[EswSequencerResponse]                                            = deriveCodec
  implicit lazy val loadScriptErrorCodec: Codec[LoadScriptError]                                          = deriveCodec

  //SequenceComponentResponse Codecs
  implicit lazy val loadScriptResponseCodec: Codec[LoadScriptResponse]             = deriveUnaryCodec
  implicit lazy val getStatusResponseCodec: Codec[GetStatusResponse]               = deriveUnaryCodec
  implicit lazy val pauseResponseCodec: Codec[PauseResponse]                       = deriveCodec
  implicit lazy val okOrUnhandledResponseCodec: Codec[OkOrUnhandledResponse]       = deriveCodec
  implicit lazy val genericResponseCodec: Codec[GenericResponse]                   = deriveCodec
  implicit lazy val removeBreakpointResponseCodec: Codec[RemoveBreakpointResponse] = deriveCodec
  implicit lazy val goOnlineResponseCodec: Codec[GoOnlineResponse]                 = deriveCodec
  implicit lazy val goOfflineResponseCodec: Codec[GoOfflineResponse]               = deriveCodec
  implicit lazy val diagnosticModeResponseCodec: Codec[DiagnosticModeResponse]     = deriveCodec
  implicit lazy val operationsModeResponseCodec: Codec[OperationsModeResponse]     = deriveCodec
  implicit lazy val loadSequenceResponseCodec: Codec[LoadSequenceResponse]         = deriveCodec
  implicit lazy val sequenceResponseCodec: Codec[SequenceResponse]                 = deriveCodec
}
