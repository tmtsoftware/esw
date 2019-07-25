package esw.http.core.commons

import akka.NotUsed
import akka.http.scaladsl.model.ws.{BinaryMessage, Message}
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import io.bullet.borer.compat.akka._
import io.bullet.borer.{Encoder, Json}

import scala.concurrent.Future

object RichMessageExt {
  implicit class ToMessage[T: Encoder](x: T) {
    def toMessage: Message = BinaryMessage(Json.encode(x).to[ByteString].result)
    def toMessageFlow: Flow[Message, Message, NotUsed] = {
      Flow.fromSinkAndSource[Message, T](Sink.ignore, Source.single(x)).map(_.toMessage)
    }
  }
  implicit class ToMessageFlow[T: Encoder](x: Future[T]) {
    def toMessageFlow: Flow[Message, Message, NotUsed] = {
      Flow.fromSinkAndSource[Message, T](Sink.ignore, Source.fromFuture(x)).map(_.toMessage)
    }
  }
}
