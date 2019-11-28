package esw.ocs.app

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.util.ByteString
import csw.params.commands.{CommandName, CommandResponse, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerPostRequest.Submit
import esw.ocs.api.protocol.SequencerWebsocketRequest.GetInsights
import io.bullet.borer.Json

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt

object TestApp extends App with WsRequest with HttpReq with SequencerHttpCodecs {
  val port: Int = 50116
//  subscribeInsights()
  submitSequence
}

trait ActorRuntime {
  val port: Int
  lazy implicit val actorSystem  = ActorSystem(Behaviors.empty, "main")
  lazy implicit val actorSystemU = actorSystem.toClassic
  lazy implicit val mat          = Materializer(actorSystem)
}

trait WsRequest extends ActorRuntime with SequencerHttpCodecs {
  lazy private val url = s"ws://localhost:$port/websocket-endpoint"
  lazy private val flow =
    Flow.fromSinkAndSource(
      Sink.foreach[Message] {
        case message: TextMessage =>
          message match {
            case TextMessage.Strict(text) => println(text)
            case TextMessage.Streamed(_)  => throw new RuntimeException("did not handle steamed message")
          }
        case _: BinaryMessage => throw new RuntimeException("did not expect binary message")
      },
      Source.single(TextMessage(Json.encode(GetInsights).toUtf8String))
    )

  def subscribeInsights(): Unit = {
    val (f, _) = Http().singleWebSocketRequest(WebSocketRequest.fromTargetUriString(url), flow)
    Await.result(f, 25.seconds)
//    println(wsResponse)
  }
}

trait HttpReq extends ActorRuntime with SequencerHttpCodecs {

  lazy private val sequence = Sequence(
    (1 to 20).map(i => Setup(Prefix("CSW"), CommandName(s"command-$i"), None))
  )
  lazy private val command: Submit = Submit(sequence)

  def submitSequence: Id = {
    val x = Http().singleRequest(
      HttpRequest(
        method = HttpMethods.POST,
        uri = Uri(s"http://localhost:$port/post-endpoint"),
        entity = HttpEntity(ContentTypes.`application/json`, Json.encode(command).toUtf8String)
      )
    )
    val response = Await.result(x, 5.seconds)
    val bytes    = Await.result(response.entity.dataBytes.runFold(ByteString.emptyByteString)(_ ++ _).map(_.toArray), 5.seconds)
    val decoded  = Json.decode(bytes).to[CommandResponse].value
    decoded.runId
  }
}
