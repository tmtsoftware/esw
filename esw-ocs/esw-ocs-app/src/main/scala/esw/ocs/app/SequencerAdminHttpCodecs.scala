package esw.ocs.app

import esw.ocs.api.models.codecs.OcsCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequencerAdminHttpCodecs extends OcsCodecs {

  implicit def SequencerAdminPostRequestCodec[T <: SequencerAdminPostRequest]: Codec[T] =
    SequencerAdminPostRequestValue.asInstanceOf[Codec[T]]

  lazy val SequencerAdminPostRequestValue: Codec[SequencerAdminPostRequest] = deriveCodec[SequencerAdminPostRequest]
}
