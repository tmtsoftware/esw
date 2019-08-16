package esw.ocs.api.codecs

import csw.command.client.cbor.MessageCodecs
import csw.location.api.codec.DoneCodec
import esw.ocs.api.models.StepStatus.Finished.{Failure, Success}
import esw.ocs.api.models.StepStatus._
import esw.ocs.api.models._
import esw.ocs.api.models.messages.EditorError._
import esw.ocs.api.models.messages.SequenceComponentMsg._
import esw.ocs.api.models.messages.SequenceComponentResponse.{GetStatusResponse, LoadScriptResponse}
import esw.ocs.api.models.messages.SequencerMessages._
import esw.ocs.api.models.messages._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveCodecForUnaryCaseClass
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait OcsFrameworkCodecs extends MessageCodecs with DoneCodec {
  //EswSequencerMsg Codecs
  implicit lazy val loadSequenceCodec: Codec[LoadSequence]   = deriveCodec[LoadSequence]
  implicit lazy val startSequenceCodec: Codec[StartSequence] = deriveCodec[StartSequence]
  implicit lazy val loadAndStartSequenceInternalCodec: Codec[LoadAndStartSequenceInternal] =
    deriveCodec[LoadAndStartSequenceInternal]

  implicit lazy val pullNextCodec: Codec[PullNext]                     = deriveCodecForUnaryCaseClass[PullNext]
  implicit lazy val maybeNextCodec: Codec[MaybeNext]                   = deriveCodecForUnaryCaseClass[MaybeNext]
  implicit lazy val readyToExecuteNextCodec: Codec[ReadyToExecuteNext] = deriveCodecForUnaryCaseClass[ReadyToExecuteNext]
  implicit lazy val updateCodec: Codec[Update]                         = deriveCodec[Update]
  implicit lazy val goIdleCodec: Codec[GoIdle]                         = deriveCodecForUnaryCaseClass[GoIdle]

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
  implicit lazy val goOnlineCodec: Codec[GoOnline]                       = deriveCodec[GoOnline]
  implicit lazy val goOfflineCodec: Codec[GoOffline]                     = deriveCodec[GoOffline]
  implicit lazy val goneOfflineCodec: Codec[GoneOffline]                 = deriveCodec[GoneOffline]
  implicit lazy val shutdownSequencerCodec: Codec[Shutdown]              = deriveCodec[Shutdown]
  implicit lazy val abortCodec: Codec[Abort]                             = deriveCodec[Abort]

  implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerMessage] = deriveCodec[EswSequencerMessage]
  implicit lazy val editorActionCodec: Codec[EditorAction]               = deriveCodec[EditorAction]

  //StepList Codecs
  implicit lazy val stepCodec: Codec[Step]                    = deriveCodec[Step]
  implicit lazy val stepListCodec: Codec[StepList]            = deriveCodec[StepList]
  implicit lazy val successStatusCodec: Codec[Success]        = deriveCodec[Success]
  implicit lazy val failureStatusCodec: Codec[Failure]        = deriveCodec[Failure]
  implicit lazy val pendingStatusCodec: Codec[Pending.type]   = singletonCodec(Pending)
  implicit lazy val inflightStatusCodec: Codec[InFlight.type] = singletonCodec(InFlight)
  implicit lazy val stepStatusCodec: Codec[StepStatus]        = deriveCodec[StepStatus]

  //StepListResponse Codecs
  implicit lazy val responseCodec: Codec[EswSequencerResponse]            = deriveCodec[EswSequencerResponse]
  implicit lazy val stepListResponseCodec: Codec[StepListResult]          = deriveCodec[StepListResult]
  implicit lazy val pullNextResultCodec: Codec[PullNextResult]            = deriveCodec[PullNextResult]
  implicit lazy val maybeNextResultCodec: Codec[MaybeNextResult]          = deriveCodec[MaybeNextResult]
  implicit lazy val sequenceResultCodec: Codec[SequenceResult]            = deriveCodec[SequenceResult]
  implicit lazy val okCodec: Codec[Ok.type]                               = singletonCodec(Ok)
  implicit lazy val duplicateIdsFoundCodec: Codec[DuplicateIdsFound.type] = singletonCodec(DuplicateIdsFound)
  implicit lazy val unhandledCodec: Codec[Unhandled]                      = deriveCodec[Unhandled]
  implicit lazy val idDoesNotExistCodec: Codec[IdDoesNotExist]            = deriveCodec[IdDoesNotExist]
  implicit lazy val inFlightOrFinishedStepErrorCodec: Codec[CannotOperateOnAnInFlightOrFinishedStep.type] =
    singletonCodec(CannotOperateOnAnInFlightOrFinishedStep)
  implicit lazy val handlersFailedCodec: Codec[GoOnlineFailed.type] = singletonCodec(GoOnlineFailed)

  //SequenceComponentCodecs
  implicit lazy val loadScriptCodec: Codec[LoadScript]                     = deriveCodec[LoadScript]
  implicit lazy val loadScriptErrorCodec: Codec[RegistrationError]         = deriveCodec[RegistrationError]
  implicit lazy val getStatusCodec: Codec[GetStatus]                       = deriveCodec[GetStatus]
  implicit lazy val unloadScriptCodec: Codec[UnloadScript]                 = deriveCodec[UnloadScript]
  implicit lazy val stopCodec: Codec[Stop.type]                            = singletonCodec(Stop)
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg] = deriveCodec[SequenceComponentMsg]

  //SequenceComponentResponse Codecs
  implicit lazy val loadScriptResponseCodec: Codec[LoadScriptResponse]               = deriveCodecForUnaryCaseClass[LoadScriptResponse]
  implicit lazy val getStatusResponseCodec: Codec[GetStatusResponse]                 = deriveCodecForUnaryCaseClass[GetStatusResponse]
  implicit lazy val sequenceComponentResponseCodec: Codec[SequenceComponentResponse] = deriveCodec[SequenceComponentResponse]
}
