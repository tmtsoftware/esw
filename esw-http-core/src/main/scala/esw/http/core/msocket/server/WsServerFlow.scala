package esw.http.core.msocket.server

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.Flow
import esw.http.core.msocket.api.Encoding
import io.bullet.borer.{Decoder, Encoder}

class WsServerFlow[T: Decoder: Encoder](socket: ServerSocket[T]) {

  def flow(encoding: Encoding): Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .collect {
        case BinaryMessage.Strict(data) if encoding.isBinary => encoding.decodeBinary(data)
        case TextMessage.Strict(text) if !encoding.isBinary  => encoding.decodeText(text)
      }
      .flatMapConcat(payload => encoding.strictMessageStream(socket.requestStream(payload.value)))
  }
}
