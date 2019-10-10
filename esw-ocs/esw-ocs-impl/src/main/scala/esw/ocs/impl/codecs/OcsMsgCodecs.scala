package esw.ocs.impl.codecs

import csw.command.client.cbor.MessageCodecs
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.codec.DoneCodec
import csw.params.core.formats.CommonCodecs
import esw.ocs.impl.messages.SequenceComponentMsg.{GetStatus, LoadScript, Stop, UnloadScript}
import esw.ocs.impl.messages.SequencerMessages._
import esw.ocs.impl.messages.{SequenceComponentMsg, SequencerState}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait OcsMsgCodecs extends MessageCodecs with DoneCodec with CommonCodecs {
  implicit lazy val loadSequenceCodec: Codec[LoadSequence]                                   = deriveCodec
  implicit lazy val startSequenceCodec: Codec[StartSequence]                                 = deriveCodec
  implicit lazy val queryFinalCodec: Codec[QueryFinal]                                       = deriveCodec
  implicit lazy val submitSequenceCodec: Codec[SubmitSequence]                               = deriveCodec
  implicit lazy val submitSequenceAndWaitInternalCodec: Codec[SubmitSequenceAndWaitInternal] = deriveCodec

  implicit lazy val pullNextCodec: Codec[PullNext]                     = deriveUnaryCodec
  implicit lazy val maybeNextCodec: Codec[MaybeNext]                   = deriveUnaryCodec
  implicit lazy val readyToExecuteNextCodec: Codec[ReadyToExecuteNext] = deriveUnaryCodec
  implicit lazy val updateCodec: Codec[Update]                         = deriveCodec
  implicit lazy val goIdleCodec: Codec[GoIdle]                         = deriveUnaryCodec

  implicit lazy val getSequenceCodec: Codec[GetSequence]             = deriveUnaryCodec
  implicit lazy val getSequencerStateCodec: Codec[GetSequencerState] = deriveUnaryCodec
  implicit lazy val addCodec: Codec[Add]                             = deriveCodec
  implicit lazy val prependCodec: Codec[Prepend]                     = deriveCodec
  implicit lazy val replaceCodec: Codec[Replace]                     = deriveCodec
  implicit lazy val insertAfterCodec: Codec[InsertAfter]             = deriveCodec
  implicit lazy val deleteCodec: Codec[Delete]                       = deriveCodec
  implicit lazy val resetCodec: Codec[Reset]                         = deriveUnaryCodec
  implicit lazy val addBreakpointCodec: Codec[AddBreakpoint]         = deriveCodec
  implicit lazy val removeBreakpointCodec: Codec[RemoveBreakpoint]   = deriveCodec
  implicit lazy val pauseCodec: Codec[Pause]                         = deriveUnaryCodec
  implicit lazy val resumeCodec: Codec[Resume]                       = deriveUnaryCodec
  implicit lazy val goOnlineCodec: Codec[GoOnline]                   = deriveUnaryCodec
  implicit lazy val goOnlineSuccessCodec: Codec[GoOnlineSuccess]     = deriveUnaryCodec
  implicit lazy val goOnlineFailedCodec: Codec[GoOnlineFailed]       = deriveUnaryCodec
  implicit lazy val goOfflineCodec: Codec[GoOffline]                 = deriveUnaryCodec
  implicit lazy val goneOfflineCodec: Codec[GoneOffline]             = deriveUnaryCodec
  implicit lazy val eswDiagnosticModeCodec: Codec[DiagnosticMode]    = deriveCodec
  implicit lazy val eswOperationsModeCodec: Codec[OperationsMode]    = deriveUnaryCodec
  implicit lazy val shutdownSequencerCodec: Codec[Shutdown]          = deriveUnaryCodec
  implicit lazy val shutdownCompleteCodec: Codec[ShutdownComplete]   = deriveUnaryCodec

  implicit lazy val abortSequenceCodec: Codec[AbortSequence]                 = deriveUnaryCodec
  implicit lazy val abortSequenceCompleteCodec: Codec[AbortSequenceComplete] = deriveUnaryCodec

  implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerMessage] = deriveCodec

  implicit lazy val sequencerBehaviorStateCodec: Codec[SequencerState[SequencerMsg]] = enumCodec[SequencerState[SequencerMsg]]

  //SequenceComponentCodecs
  implicit lazy val loadScriptCodec: Codec[LoadScript]                     = deriveCodec
  implicit lazy val getStatusCodec: Codec[GetStatus]                       = deriveCodec
  implicit lazy val unloadScriptCodec: Codec[UnloadScript]                 = deriveCodec
  implicit lazy val stopCodec: Codec[Stop.type]                            = deriveCodec
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentMsg] = deriveCodec
}
