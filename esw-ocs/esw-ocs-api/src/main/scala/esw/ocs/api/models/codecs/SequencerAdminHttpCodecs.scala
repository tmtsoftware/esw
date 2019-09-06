package esw.ocs.api.models.codecs

import com.github.ghik.silencer.silent
import esw.ocs.api.models.request.SequencerAdminPostRequest
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequencerAdminHttpCodecs extends OcsCodecs {

  implicit def SequencerAdminPostRequestCodec[T <: SequencerAdminPostRequest]: Codec[T] =
    SequencerAdminPostRequestValue.asInstanceOf[Codec[T]]

  lazy val SequencerAdminPostRequestValue: Codec[SequencerAdminPostRequest] = {
    @silent implicit lazy val getSequenceCodec: Codec[SequencerAdminPostRequest.GetSequence.type] = singletonCodec(
      SequencerAdminPostRequest.GetSequence
    )
    deriveCodec[SequencerAdminPostRequest]
  }
}
