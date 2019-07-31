package esw.ocs.api.codecs

import csw.command.client.cbor.MessageCodecs
import csw.location.api.codec.DoneCodec
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus._
import esw.ocs.api.models._
import esw.ocs.api.models.messages.EditorError._
import esw.ocs.api.models.messages.SequenceComponentMsg.{GetStatus, LoadScript, UnloadScript}
import esw.ocs.api.models.messages.SequenceComponentResponses.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.messages.SequencerMessages.{ExternalEditorMsg, _}
import esw.ocs.api.models.messages.SequencerResponses.{EditorResponse, LifecycleResponse, LoadSequenceResponse, StepListResponse}
import esw.ocs.api.models.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait OcsFrameworkCodecs extends MessageCodecs with DoneCodec {
  implicit lazy val startSequenceCodec: Codec[StartSequence]               = deriveCodecForUnaryCaseClass[StartSequence]
  implicit lazy val loadSequenceCodec: Codec[LoadSequence]                 = deriveCodec[LoadSequence]
  implicit lazy val loadSequenceResponseCodec: Codec[LoadSequenceResponse] = deriveCodec[LoadSequenceResponse]

  //LifecycleMsg Codecs
  implicit lazy val goOnlineCodec: Codec[GoOnline]          = deriveCodec[GoOnline]
  implicit lazy val goOfflineCodec: Codec[GoOffline]        = deriveCodec[GoOffline]
  implicit lazy val shutdownSequencerCodec: Codec[Shutdown] = deriveCodec[Shutdown]
  implicit lazy val abortCodec: Codec[Abort]                = deriveCodec[Abort]
  implicit lazy val lifecycleMsgCodec: Codec[LifecycleMsg]  = deriveCodec[LifecycleMsg]

  //LifecycleResponse Codecs

  implicit lazy val lifecycleResponseCodec: Codec[LifecycleResponse] = deriveCodecForUnaryCaseClass[LifecycleResponse]

  //ExternalEditorSequencerMsg Codecs

  implicit lazy val availableCodec: Codec[Available]                     = deriveCodec[Available]
  implicit lazy val getSequenceCodec: Codec[GetSequence]                 = deriveCodec[GetSequence]
  implicit lazy val getPreviousSequenceCodec: Codec[GetPreviousSequence] = deriveCodec[GetPreviousSequence]
  implicit lazy val addCodec: Codec[Add]                                 = deriveCodec[Add]
  implicit lazy val prependCodec: Codec[Prepend]                         = deriveCodec[Prepend]
  implicit lazy val replaceCodec: Codec[Replace]                         = deriveCodec[Replace]
  implicit lazy val insertAfterCodec: Codec[InsertAfter]                 = deriveCodec[InsertAfter]
  implicit lazy val deleteCodec: Codec[Delete]                           = deriveCodec[Delete]
  implicit lazy val addBreakpointCodec: Codec[AddBreakpoint]             = deriveCodec[AddBreakpoint]
  implicit lazy val removeBreakpointCodec: Codec[RemoveBreakpoint]       = deriveCodec[RemoveBreakpoint]
  implicit lazy val pauseCodec: Codec[Pause]                             = deriveCodec[Pause]
  implicit lazy val resumeCodec: Codec[Resume]                           = deriveCodec[Resume]
  implicit lazy val resetCodec: Codec[Reset]                             = deriveCodec[Reset]
  implicit lazy val externalEditorMsgCodec: Codec[ExternalEditorMsg]     = deriveCodec[ExternalEditorMsg]

  //StepList Codecs

  implicit lazy val stepCodec: Codec[Step]         = deriveCodec[Step]
  implicit lazy val stepListCodec: Codec[StepList] = deriveCodec[StepList]

  implicit lazy val successStatusCodec: Codec[Success] = deriveCodec[Success]
  implicit lazy val failureStatusCodec: Codec[Failure] = deriveCodec[Failure]

  implicit lazy val pendingStatusCodec: Codec[Pending.type]   = singletonCodec(Pending)
  implicit lazy val inflightStatusCodec: Codec[InFlight.type] = singletonCodec(InFlight)
  implicit lazy val finishedStatusCodec: Codec[Finished]      = deriveCodec[Finished]

  implicit lazy val stepStatusCodec: Codec[StepStatus] = deriveCodec[StepStatus]

  //StepListResponse Codecs
  implicit lazy val stepListResponseCodec: Codec[StepListResponse] = deriveCodec[StepListResponse]

  //SequencerErrorCodecs
  implicit lazy val notSupportedCodec: Codec[NotSupported] = deriveCodec[NotSupported]
  implicit lazy val notAllowedOnFinishedSeqCodec: Codec[NotAllowedOnFinishedSeq.type] =
    singletonCodec(NotAllowedOnFinishedSeq)
  implicit lazy val notAllowedInOfflineStateCodec: Codec[NotAllowedInOfflineState.type] =
    singletonCodec(NotAllowedInOfflineState)
  implicit lazy val idDoesNotExistCodec: Codec[IdDoesNotExist]         = deriveCodec[IdDoesNotExist]
  implicit lazy val pauseFailedCodec: Codec[PauseFailed]               = deriveCodecForUnaryCaseClass[PauseFailed]
  implicit lazy val updateNotSupportedCodec: Codec[UpdateNotSupported] = deriveCodec[UpdateNotSupported]

  implicit lazy val goOnlineErrorCodec: Codec[GoOnlineError]          = deriveCodec[GoOnlineError]
  implicit lazy val goOfflineErrorCodec: Codec[GoOfflineError]        = deriveCodec[GoOfflineError]
  implicit lazy val sequencerShutdownErrorCodec: Codec[ShutdownError] = deriveCodec[ShutdownError]
  implicit lazy val sequencerAbortErrorCodec: Codec[AbortError]       = deriveCodec[AbortError]

  implicit lazy val lifecycleErrorCodec: Codec[LifecycleError] = deriveCodec[LifecycleError]
  implicit lazy val editorErrorCodec: Codec[EditorError]       = deriveCodec[EditorError]

  //SequenceEditorResponse Codecs
  implicit lazy val editorResponseCodec: Codec[EditorResponse] = deriveCodecForUnaryCaseClass[EditorResponse]

  //SequenceComponentCodecs
  implicit lazy val loadScriptCodec: Codec[LoadScript]                     = deriveCodec[LoadScript]
  implicit lazy val loadScriptErrorCodec: Codec[RegistrationError]         = deriveCodec[RegistrationError]
  implicit lazy val getStatusCodec: Codec[GetStatus]                       = deriveCodec[GetStatus]
  implicit lazy val unloadScriptCodec: Codec[UnloadScript]                 = deriveCodec[UnloadScript]
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg] = deriveCodec[SequenceComponentMsg]

  //SequenceComponentResponse Codecs
  implicit lazy val loadScriptResponseCodec: Codec[LoadScriptResponse] = deriveCodecForUnaryCaseClass[LoadScriptResponse]
  implicit lazy val getStatusResponseCodec: Codec[GetStatusResponse]   = deriveCodecForUnaryCaseClass[GetStatusResponse]

  //fixme:  check if it works without DoneCodecs and LocationCodecs and ActorRefCodec and types wrapped inside Option and Either
}
