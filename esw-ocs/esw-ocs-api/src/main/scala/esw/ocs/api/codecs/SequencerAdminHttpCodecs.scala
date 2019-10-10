package esw.ocs.api.codecs

import com.github.ghik.silencer.silent
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import esw.ocs.api.protocol.SequencerAdminWebsocketRequest.QueryFinal
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerAdminWebsocketRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequencerAdminHttpCodecs extends OcsCodecs {

  implicit def sequencerAdminPostRequestCodec[T <: SequencerAdminPostRequest]: Codec[T] =
    sequencerAdminPostRequestValue.asInstanceOf[Codec[T]]

  lazy val sequencerAdminPostRequestValue: Codec[SequencerAdminPostRequest] = {
    @silent implicit lazy val getSequenceCodec: Codec[GetSequence.type]       = deriveCodec[GetSequence.type]
    @silent implicit lazy val isAvailableCodec: Codec[IsAvailable.type]       = deriveCodec[IsAvailable.type]
    @silent implicit lazy val isOnlineCodec: Codec[IsOnline.type]             = deriveCodec[IsOnline.type]
    @silent implicit lazy val pauseCodec: Codec[Pause.type]                   = deriveCodec[Pause.type]
    @silent implicit lazy val resumeCodec: Codec[Resume.type]                 = deriveCodec[Resume.type]
    @silent implicit lazy val resetCodec: Codec[Reset.type]                   = deriveCodec[Reset.type]
    @silent implicit lazy val abortSequenceCodec: Codec[AbortSequence.type]   = deriveCodec[AbortSequence.type]
    @silent implicit lazy val goOnlineCodec: Codec[GoOnline.type]             = deriveCodec[GoOnline.type]
    @silent implicit lazy val goOfflineCodec: Codec[GoOffline.type]           = deriveCodec[GoOffline.type]
    @silent implicit lazy val startSequenceCodec: Codec[StartSequence.type]   = deriveCodec[StartSequence.type]
    @silent implicit lazy val operationsModeCodec: Codec[OperationsMode.type] = deriveCodec[OperationsMode.type]
    @silent implicit lazy val diagnosticModeCodec: Codec[DiagnosticMode]      = deriveCodec[DiagnosticMode]
    @silent implicit lazy val loadCodec: Codec[LoadSequence]                  = deriveCodec[LoadSequence]
    @silent implicit lazy val submitSequenceCodec: Codec[SubmitSequence]      = deriveCodec[SubmitSequence]
    @silent implicit lazy val addCodec: Codec[Add]                            = deriveCodec[Add]
    @silent implicit lazy val prependCodec: Codec[Prepend]                    = deriveCodec[Prepend]
    @silent implicit lazy val replaceCodec: Codec[Replace]                    = deriveCodec[Replace]
    @silent implicit lazy val insertAfterCodec: Codec[InsertAfter]            = deriveCodec[InsertAfter]
    @silent implicit lazy val deleteCodec: Codec[Delete]                      = deriveCodec[Delete]
    @silent implicit lazy val addBreakpointCodec: Codec[AddBreakpoint]        = deriveCodec[AddBreakpoint]
    @silent implicit lazy val removeBreakpointCodec: Codec[RemoveBreakpoint]  = deriveCodec[RemoveBreakpoint]

    deriveCodec[SequencerAdminPostRequest]
  }

  implicit def sequencerAdminWebsocketRequestCodec[T <: SequencerAdminWebsocketRequest]: Codec[T] =
    sequencerAdminWebsocketRequestValue.asInstanceOf[Codec[T]]

  lazy val sequencerAdminWebsocketRequestValue: Codec[SequencerAdminWebsocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal.type] = deriveCodec[QueryFinal.type]
    deriveCodec[SequencerAdminWebsocketRequest]
  }
}
