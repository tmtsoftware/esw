package msocket.api

import akka.NotUsed
import akka.stream.scaladsl.Source

trait ServerSocket[Req] {
  def requestStream(request: Req): Source[Payload[_], NotUsed]
}
