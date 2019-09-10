package esw.ocs.api.codecs

import com.github.ghik.silencer.silent
import esw.ocs.api.request.SequencerAdminPostRequest
import esw.ocs.api.request.SequencerAdminPostRequest._
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequencerAdminHttpCodecs extends OcsCodecs {

  implicit def SequencerAdminPostRequestCodec[T <: SequencerAdminPostRequest]: Codec[T] =
    SequencerAdminPostRequestValue.asInstanceOf[Codec[T]]

  lazy val SequencerAdminPostRequestValue: Codec[SequencerAdminPostRequest] = {
    @silent implicit lazy val getSequenceCodec: Codec[GetSequence.type]      = singletonCodec(GetSequence)
    @silent implicit lazy val isAvailableCodec: Codec[IsAvailable.type]      = singletonCodec(IsAvailable)
    @silent implicit lazy val isOnlineCodec: Codec[IsOnline.type]            = singletonCodec(IsOnline)
    @silent implicit lazy val pauseCodec: Codec[Pause.type]                  = singletonCodec(Pause)
    @silent implicit lazy val resumeCodec: Codec[Resume.type]                = singletonCodec(Resume)
    @silent implicit lazy val resetCodec: Codec[Reset.type]                  = singletonCodec(Reset)
    @silent implicit lazy val abortSequenceCodec: Codec[AbortSequence.type]  = singletonCodec(AbortSequence)
    @silent implicit lazy val goOnlineCodec: Codec[GoOnline.type]            = singletonCodec(GoOnline)
    @silent implicit lazy val goOfflineCodec: Codec[GoOffline.type]          = singletonCodec(GoOffline)
    @silent implicit lazy val addCodec: Codec[Add]                           = deriveCodec[Add]
    @silent implicit lazy val prependCodec: Codec[Prepend]                   = deriveCodec[Prepend]
    @silent implicit lazy val replaceCodec: Codec[Replace]                   = deriveCodec[Replace]
    @silent implicit lazy val insertAfterCodec: Codec[InsertAfter]           = deriveCodec[InsertAfter]
    @silent implicit lazy val deleteCodec: Codec[Delete]                     = deriveCodec[Delete]
    @silent implicit lazy val addBreakpointCodec: Codec[AddBreakpoint]       = deriveCodec[AddBreakpoint]
    @silent implicit lazy val removeBreakpointCodec: Codec[RemoveBreakpoint] = deriveCodec[RemoveBreakpoint]

    deriveCodec[SequencerAdminPostRequest]
  }
}
