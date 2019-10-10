package esw.ocs.impl.codecs

import csw.command.client.cbor.MessageCodecs
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.codec.DoneCodec
import esw.ocs.impl.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.{SequenceComponentMsg, SequencerState}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait OcsMsgCodecs extends MessageCodecs with DoneCodec {
  implicit lazy val loadSequenceCodec: Codec[LoadSequence]     = deriveCodec[LoadSequence]
  implicit lazy val startSequenceCodec: Codec[StartSequence]   = deriveCodec[StartSequence]
  implicit lazy val queryFinalCodec: Codec[QueryFinal]         = deriveCodec[QueryFinal]
  implicit lazy val submitSequenceCodec: Codec[SubmitSequence] = deriveCodec[SubmitSequence]
  implicit lazy val submitSequenceAndWaitInternalCodec: Codec[SubmitSequenceAndWaitInternal] =
    deriveCodec[SubmitSequenceAndWaitInternal]

  implicit lazy val pullNextCodec: Codec[PullNext]                     = deriveUnaryCodec[PullNext]
  implicit lazy val maybeNextCodec: Codec[MaybeNext]                   = deriveUnaryCodec[MaybeNext]
  implicit lazy val readyToExecuteNextCodec: Codec[ReadyToExecuteNext] = deriveUnaryCodec[ReadyToExecuteNext]
  implicit lazy val updateCodec: Codec[Update]                         = deriveCodec[Update]
  implicit lazy val goIdleCodec: Codec[GoIdle]                         = deriveUnaryCodec[GoIdle]

  implicit lazy val getSequenceCodec: Codec[GetSequence]             = deriveUnaryCodec[GetSequence]
  implicit lazy val getSequencerStateCodec: Codec[GetSequencerState] = deriveUnaryCodec[GetSequencerState]
  implicit lazy val addCodec: Codec[Add]                             = deriveCodec[Add]
  implicit lazy val prependCodec: Codec[Prepend]                     = deriveCodec[Prepend]
  implicit lazy val replaceCodec: Codec[Replace]                     = deriveCodec[Replace]
  implicit lazy val insertAfterCodec: Codec[InsertAfter]             = deriveCodec[InsertAfter]
  implicit lazy val deleteCodec: Codec[Delete]                       = deriveCodec[Delete]
  implicit lazy val resetCodec: Codec[Reset]                         = deriveUnaryCodec[Reset]
  implicit lazy val addBreakpointCodec: Codec[AddBreakpoint]         = deriveCodec[AddBreakpoint]
  implicit lazy val removeBreakpointCodec: Codec[RemoveBreakpoint]   = deriveCodec[RemoveBreakpoint]
  implicit lazy val pauseCodec: Codec[Pause]                         = deriveUnaryCodec[Pause]
  implicit lazy val resumeCodec: Codec[Resume]                       = deriveUnaryCodec[Resume]
  implicit lazy val goOnlineCodec: Codec[GoOnline]                   = deriveUnaryCodec[GoOnline]
  implicit lazy val goOnlineSuccessCodec: Codec[GoOnlineSuccess]     = deriveUnaryCodec[GoOnlineSuccess]
  implicit lazy val goOnlineFailedCodec: Codec[GoOnlineFailed]       = deriveUnaryCodec[GoOnlineFailed]
  implicit lazy val goOfflineCodec: Codec[GoOffline]                 = deriveUnaryCodec[GoOffline]
  implicit lazy val goneOfflineCodec: Codec[GoneOffline]             = deriveUnaryCodec[GoneOffline]
  implicit lazy val eswDiagnosticModeCodec: Codec[DiagnosticMode]    = deriveCodec[DiagnosticMode]
  implicit lazy val eswOperationsModeCodec: Codec[OperationsMode]    = deriveUnaryCodec[OperationsMode]
  implicit lazy val shutdownSequencerCodec: Codec[Shutdown]          = deriveUnaryCodec[Shutdown]
  implicit lazy val shutdownCompleteCodec: Codec[ShutdownComplete]   = deriveUnaryCodec[ShutdownComplete]

  implicit lazy val abortSequenceCodec: Codec[AbortSequence]                 = deriveUnaryCodec[AbortSequence]
  implicit lazy val abortSequenceCompleteCodec: Codec[AbortSequenceComplete] = deriveUnaryCodec[AbortSequenceComplete]

  implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerMessage] = deriveCodec[EswSequencerMessage]

  implicit lazy val sequencerBehaviorStateCodec: Codec[SequencerState[SequencerMsg]] = enumCodec[SequencerState[SequencerMsg]]

  //SequenceComponentCodecs
  implicit lazy val loadScriptCodec: Codec[LoadScript]     = deriveCodec[LoadScript]
  implicit lazy val getStatusCodec: Codec[GetStatus]       = deriveCodec[GetStatus]
  implicit lazy val unloadScriptCodec: Codec[UnloadScript] = deriveCodec[UnloadScript]
  implicit lazy val stopCodec: Codec[Stop.type]            = deriveCodec[Stop.type]

  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg] = deriveCodec[SequenceComponentMsg]

}
