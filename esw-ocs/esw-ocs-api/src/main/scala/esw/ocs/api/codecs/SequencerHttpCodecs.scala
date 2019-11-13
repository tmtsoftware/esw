package esw.ocs.api.codecs

import com.github.ghik.silencer.silent
import esw.ocs.api.protocol.SequencerAdminPostRequest._
import esw.ocs.api.protocol.SequencerCommandPostRequest.{LoadSequence, StartSequence, SubmitSequence}
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest.QueryFinal
import esw.ocs.api.protocol.{SequencerAdminPostRequest, SequencerCommandPostRequest, SequencerCommandWebsocketRequest}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequencerHttpCodecs extends OcsCodecs {

  implicit def sequencerAdminPostRequestCodec[T <: SequencerAdminPostRequest]: Codec[T] =
    sequencerAdminPostRequestValue.asInstanceOf[Codec[T]]

  lazy val sequencerAdminPostRequestValue: Codec[SequencerAdminPostRequest] = {
    @silent implicit lazy val getSequenceCodec: Codec[GetSequence.type]       = deriveCodec
    @silent implicit lazy val isAvailableCodec: Codec[IsAvailable.type]       = deriveCodec
    @silent implicit lazy val isOnlineCodec: Codec[IsOnline.type]             = deriveCodec
    @silent implicit lazy val pauseCodec: Codec[Pause.type]                   = deriveCodec
    @silent implicit lazy val resumeCodec: Codec[Resume.type]                 = deriveCodec
    @silent implicit lazy val resetCodec: Codec[Reset.type]                   = deriveCodec
    @silent implicit lazy val abortSequenceCodec: Codec[AbortSequence.type]   = deriveCodec
    @silent implicit lazy val stopCodec: Codec[Stop.type]                     = deriveCodec
    @silent implicit lazy val goOnlineCodec: Codec[GoOnline.type]             = deriveCodec
    @silent implicit lazy val goOfflineCodec: Codec[GoOffline.type]           = deriveCodec
    @silent implicit lazy val operationsModeCodec: Codec[OperationsMode.type] = deriveCodec
    @silent implicit lazy val diagnosticModeCodec: Codec[DiagnosticMode]      = deriveCodec
    @silent implicit lazy val loadCodec: Codec[LoadSequence]                  = deriveCodec
    @silent implicit lazy val addCodec: Codec[Add]                            = deriveCodec
    @silent implicit lazy val prependCodec: Codec[Prepend]                    = deriveCodec
    @silent implicit lazy val replaceCodec: Codec[Replace]                    = deriveCodec
    @silent implicit lazy val insertAfterCodec: Codec[InsertAfter]            = deriveCodec
    @silent implicit lazy val deleteCodec: Codec[Delete]                      = deriveCodec
    @silent implicit lazy val addBreakpointCodec: Codec[AddBreakpoint]        = deriveCodec
    @silent implicit lazy val removeBreakpointCodec: Codec[RemoveBreakpoint]  = deriveCodec
    deriveCodec
  }

  implicit def sequencerCommandPostRequestCodec[T <: SequencerCommandPostRequest]: Codec[T] =
    sequencerCommandPostRequestValue.asInstanceOf[Codec[T]]

  lazy val sequencerCommandPostRequestValue: Codec[SequencerCommandPostRequest] = {
    @silent implicit lazy val submitSequenceCodec: Codec[SubmitSequence]    = deriveCodec
    @silent implicit lazy val loadSequenceCodec: Codec[LoadSequence]        = deriveCodec
    @silent implicit lazy val startSequenceCodec: Codec[StartSequence.type] = deriveCodec
    deriveCodec
  }

  implicit def sequencerCommandWebsocketRequestCodec[T <: SequencerCommandWebsocketRequest]: Codec[T] =
    sequencerCommandWebsocketRequestValue.asInstanceOf[Codec[T]]

  lazy val sequencerCommandWebsocketRequestValue: Codec[SequencerCommandWebsocketRequest] = {
    @silent implicit lazy val queryFinalCodec: Codec[QueryFinal.type] = deriveCodec
    deriveCodec
  }
}
