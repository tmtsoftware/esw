package esw.http.core.msocket.server

import akka.NotUsed
import akka.stream.scaladsl.Source
import esw.http.core.msocket.api.Payload

trait ServerSocket[Req] {
  def requestStream(request: Req): Source[Payload[_], NotUsed]
}
