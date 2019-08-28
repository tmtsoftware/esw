package mscoket.impl

import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.scaladsl.{Flow, Source}
import io.bullet.borer.{Decoder, Encoder}
import mscoket.impl.Encoding.JsonText
import msocket.api.RequestHandler

class WsServerFlow[T: Decoder: Encoder](websocketHandler: RequestHandler[T, Source[Message, NotUsed]]) {

  val flow: Flow[Message, Message, NotUsed] = {
    Flow[Message]
      .collect {
        case TextMessage.Strict(text) => JsonText.decodeText(text)
      }
      .flatMapConcat(websocketHandler.handle)
  }
}
