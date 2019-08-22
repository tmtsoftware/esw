package msocket.api

import io.bullet.borer.{Decoder, Encoder}

import scala.concurrent.Future

trait HttpClient {
  def post[Req: Encoder, Res: Decoder](req: Req): Future[Res]
}
