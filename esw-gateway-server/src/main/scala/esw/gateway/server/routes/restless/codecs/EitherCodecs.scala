package esw.gateway.server.routes.restless.codecs

import csw.params.core.formats.CodecHelpers
import io.bullet.borer.{Codec, Decoder, Encoder}

trait EitherCodecs {
  implicit def eitherCodec[E: Encoder: Decoder, S: Encoder: Decoder]: Codec[Either[E, S]] = {
    CodecHelpers.bimap[Result[S, E], Either[E, S]](_.toEither, Result.fromEither)
  }

  implicit def eitherEnc[E: Encoder: Decoder, S: Encoder: Decoder]: Encoder[Either[E, S]] = eitherCodec[E, S].encoder
  implicit def eitherDec[E: Encoder: Decoder, S: Encoder: Decoder]: Decoder[Either[E, S]] = eitherCodec[E, S].decoder
}
