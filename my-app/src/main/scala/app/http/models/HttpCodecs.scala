package app.http.models

import io.bullet.borer.Codec
import io.bullet.borer.compat.AkkaHttpCompat
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

object HttpCodecs extends HttpCodecs

trait HttpCodecs extends AkkaHttpCompat {
  implicit lazy val sampleResponseCodec: Codec[ServerResponse] = deriveCodec
}
