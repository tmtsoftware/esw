package esw.ocs.admin.api

import com.github.ghik.silencer.silent
import esw.ocs.admin.api.SequencerAdminPostRequest.GetSequence
import esw.ocs.api.models.codecs.OcsCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

trait SequencerAdminHttpCodecs extends OcsCodecs {

  implicit def SequencerAdminPostRequestCodec[T <: SequencerAdminPostRequest]: Codec[T] =
    SequencerAdminPostRequestValue.asInstanceOf[Codec[T]]
  lazy val SequencerAdminPostRequestValue: Codec[SequencerAdminPostRequest] = {
    @silent implicit lazy val setAlarmSeverityCodec: Codec[GetSequence] = deriveCodec[GetSequence]
    deriveCodec[SequencerAdminPostRequest]
  }
}
