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

trait OcsCodecs extends ParamCodecs with LocationCodecs {

  def singletonErrorCodec[T <: SingletonError with Singleton](a: T): Codec[T] =
    Codec.bimap[String, T](_.msg, _ => a)

  //StepList Codecs
  implicit lazy val stepCodec: Codec[Step]                    = deriveCodec[Step]
  implicit lazy val stepListCodec: Codec[StepList]            = deriveCodec[StepList]
  implicit lazy val successStatusCodec: Codec[Success]        = deriveCodec[Success]
  implicit lazy val failureStatusCodec: Codec[Failure]        = deriveCodec[Failure]
  implicit lazy val pendingStatusCodec: Codec[Pending.type]   = singletonCodec(Pending)
  implicit lazy val inflightStatusCodec: Codec[InFlight.type] = singletonCodec(InFlight)
  implicit lazy val stepStatusCodec: Codec[StepStatus]        = deriveCodec[StepStatus]

  //StepListResponse Codecs
  implicit lazy val pullNextResultCodec: Codec[PullNextResult] = deriveCodec[PullNextResult]
  implicit lazy val sequenceResultCodec: Codec[SequenceResult] = deriveCodec[SequenceResult]
  implicit lazy val okCodec: Codec[Ok.type]                    = singletonCodec(Ok)

  implicit lazy val unhandledCodec: Codec[Unhandled]           = deriveCodec[Unhandled]
  implicit lazy val idDoesNotExistCodec: Codec[IdDoesNotExist] = deriveCodec[IdDoesNotExist]
  implicit lazy val inFlightOrFinishedStepErrorCodec: Codec[CannotOperateOnAnInFlightOrFinishedStep.type] =
    singletonCodec(CannotOperateOnAnInFlightOrFinishedStep)
  implicit lazy val duplicateIdsFoundCodec: Codec[DuplicateIdsFound.type]       = singletonErrorCodec(DuplicateIdsFound)
  implicit lazy val goOnlineHookFailedCodec: Codec[GoOnlineHookFailed.type]     = singletonErrorCodec(GoOnlineHookFailed)
  implicit lazy val diagnosticHookFailedCodec: Codec[DiagnosticHookFailed.type] = singletonErrorCodec(DiagnosticHookFailed)
  implicit lazy val operationsHookFailedCodec: Codec[OperationsHookFailed.type] = singletonErrorCodec(OperationsHookFailed)
  implicit lazy val responseCodec: Codec[EswSequencerResponse]                  = deriveCodec[EswSequencerResponse]

  implicit lazy val loadScriptErrorCodec: Codec[RegistrationError] = deriveCodec[RegistrationError]

  //SequenceComponentResponse Codecs
  implicit lazy val loadScriptResponseCodec: Codec[LoadScriptResponse]             = deriveUnaryCodec[LoadScriptResponse]
  implicit lazy val getStatusResponseCodec: Codec[GetStatusResponse]               = deriveUnaryCodec[GetStatusResponse]
  implicit lazy val pauseResponseCodec: Codec[PauseResponse]                       = deriveCodec[PauseResponse]
  implicit lazy val okOrUnhandledResponseCodec: Codec[OkOrUnhandledResponse]       = deriveCodec[OkOrUnhandledResponse]
  implicit lazy val genericResponseCodec: Codec[GenericResponse]                   = deriveCodec[GenericResponse]
  implicit lazy val removeBreakpointResponseCodec: Codec[RemoveBreakpointResponse] = deriveCodec[RemoveBreakpointResponse]
  implicit lazy val goOnlineResponseCodec: Codec[GoOnlineResponse]                 = deriveCodec[GoOnlineResponse]
  implicit lazy val diagnosticModeResponseCodec: Codec[DiagnosticModeResponse]     = deriveCodec[DiagnosticModeResponse]
  implicit lazy val operationsModeResponseCodec: Codec[OperationsModeResponse]     = deriveCodec[OperationsModeResponse]
  implicit lazy val loadSequenceResponseCodec: Codec[LoadSequenceResponse]         = deriveCodec[LoadSequenceResponse]
  implicit lazy val sequenceResponseCodec: Codec[SequenceResponse]                 = deriveCodec[SequenceResponse]
}
