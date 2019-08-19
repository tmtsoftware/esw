package esw.http.core.codec

import csw.commons.http.{ErrorMessage, ErrorResponse}
import io.bullet.borer.Codec
import io.bullet.borer.derivation.ArrayBasedCodecs.deriveUnaryCodec
import io.bullet.borer.derivation.MapBasedCodecs.deriveCodec

object ErrorCodecs {
  implicit lazy val ErrorResponseCodec: Codec[ErrorResponse] = deriveUnaryCodec[ErrorResponse]
  implicit lazy val ErrorMessageCodec: Codec[ErrorMessage]   = deriveCodec[ErrorMessage]
}
