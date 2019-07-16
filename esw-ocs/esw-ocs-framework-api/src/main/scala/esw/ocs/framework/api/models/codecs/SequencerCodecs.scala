package esw.ocs.framework.api.models.codecs

import csw.location.api.codec.DoneCodec
import csw.params.core.formats.{CborHelpers, ParamCodecs}
import esw.ocs.framework.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.framework.api.models.StepStatus._
import esw.ocs.framework.api.models.messages.ProcessSequenceError.{DuplicateIdsFound, ExistingSequenceIsInProcess}
import esw.ocs.framework.api.models.messages.StepListError.{AddBreakpointError, AddError, PauseError, _}
import esw.ocs.framework.api.models.messages.{ProcessSequenceError, StepListError}
import esw.ocs.framework.api.models.{Sequence, Step, StepList, StepStatus}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

class SequencerCodecs extends ParamCodecs with DoneCodec {

  def singletonCodec[T <: Singleton](a: T): Codec[T] = CborHelpers.bimap[String, T](_ => a, _.toString)
  // StepCodecs
  implicit lazy val successStatusCode: Codec[Success] = deriveCodec[Success]
  implicit lazy val failureStatusCode: Codec[Failure] = deriveCodec[Failure]

  implicit lazy val pendingStatusCodec: Codec[Pending.type]   = singletonCodec(Pending)
  implicit lazy val inflightStatusCodec: Codec[InFlight.type] = singletonCodec(InFlight)
  implicit lazy val finishedStatusCodec: Codec[Finished]      = deriveCodec[Finished]

  implicit lazy val stepStatusCodec: Codec[StepStatus] = deriveCodec[StepStatus]

  implicit lazy val stepCodec: Codec[Step]         = deriveCodec[Step]
  implicit lazy val stepListCodec: Codec[StepList] = deriveCodec[StepList]

  //SequenceCodec
  implicit lazy val sequenceCodec: Codec[Sequence] = deriveCodec[Sequence]

  //ProcessSequenceErrorCodecs
  implicit lazy val duplicateIdsFoundCodec: Codec[DuplicateIdsFound.type] = singletonCodec(DuplicateIdsFound)
  implicit lazy val existingSequenceIsInProcessCodec: Codec[ExistingSequenceIsInProcess.type] =
    singletonCodec(ExistingSequenceIsInProcess)
  implicit lazy val processSequenceErrorCodec: Codec[ProcessSequenceError] = deriveCodec[ProcessSequenceError]

  //StepListErrorCodecs

  implicit lazy val notSupportedCodec: Codec[NotSupported] = deriveCodec[NotSupported]
  implicit lazy val notAllowedOnFinishedSeqCodec: Codec[NotAllowedOnFinishedSeq.type] =
    singletonCodec(NotAllowedOnFinishedSeq)
  implicit lazy val idDoesNotExistCodec: Codec[IdDoesNotExist] = deriveCodec[IdDoesNotExist]
  implicit lazy val addingBreakpointNotSupportedCodec: Codec[AddingBreakpointNotSupported] =
    deriveCodec[AddingBreakpointNotSupported]
  implicit lazy val pauseFailedCodec: Codec[PauseFailed.type]                = singletonCodec(PauseFailed)
  implicit lazy val pauseErrorCodec: Codec[PauseError]                       = deriveCodec[PauseError]
  implicit lazy val updateNotSupportedCodec: Codec[UpdateNotSupported]       = deriveCodec[UpdateNotSupported]
  implicit lazy val updateErrorCodec: Codec[UpdateError]                     = deriveCodec[UpdateError]
  implicit lazy val addFailedCodec: Codec[AddFailed.type]                    = singletonCodec(AddFailed)
  implicit lazy val addErrorCodec: Codec[AddError]                           = deriveCodec[AddError]
  implicit lazy val resumeErrorCodec: Codec[ResumeError]                     = deriveCodec[ResumeError]
  implicit lazy val resetErrorCodec: Codec[ResetError]                       = deriveCodec[ResetError]
  implicit lazy val replaceErrorCodec: Codec[ReplaceError]                   = deriveCodec[ReplaceError]
  implicit lazy val prependErrorCodec: Codec[PrependError]                   = deriveCodec[PrependError]
  implicit lazy val deleteErrorCodec: Codec[DeleteError]                     = deriveCodec[DeleteError]
  implicit lazy val insertErrorCodec: Codec[InsertError]                     = deriveCodec[InsertError]
  implicit lazy val removeBreakpointErrorCodec: Codec[RemoveBreakpointError] = deriveCodec[RemoveBreakpointError]
  implicit lazy val addBreakpointErrorCodec: Codec[AddBreakpointError]       = deriveCodec[AddBreakpointError]

  implicit lazy val stepListErrorCodec: Codec[StepListError.type] = singletonCodec(StepListError)
}
