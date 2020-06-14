package esw.ocs.handler

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.util.Timeout
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerWebsocketRequest
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.api.SequencerApi
import io.bullet.borer.Decoder
import msocket.api.ContentEncoding.JsonText
import msocket.api.ContentType
import msocket.impl.CborByteString
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.ws.WebsocketExtensions.WebsocketEncoding
import msocket.impl.ws.WebsocketRouteFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import esw.commons.BaseTestSuite

class SequencerWebsocketHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerHttpCodecs
    with ClientHttpCodecs {

  override def clientContentType: ContentType = ContentType.Json

  private val sequencer: SequencerApi = mock[SequencerApi]

  private def websocketHandlerFactory(contentType: ContentType) = new SequencerWebsocketHandler(sequencer, contentType)

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")

  lazy val route: Route =
    new WebsocketRouteFactory[SequencerWebsocketRequest]("websocket-endpoint", websocketHandlerFactory).make()

  private val wsClient = WSProbe()

  "SequencerWebsocketHandler" must {
    "return final submit response of sequence for QueryFinal request | ESW-101" in {
      val id                        = Id("some")
      implicit val timeout: Timeout = Timeout(10.seconds)
      val completedResponse         = Completed(id)
      when(sequencer.queryFinal(id)).thenReturn(Future.successful(completedResponse))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(QueryFinal(id, timeout): SequencerWebsocketRequest))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[SubmitResponse](wsClient)
        response shouldEqual completedResponse

      }
    }
  }

  private def decodeMessage[T](wsClient: WSProbe)(implicit decoder: Decoder[T]): T = {
    wsClient.expectMessage() match {
      case TextMessage.Strict(text)   => JsonText.decode[T](text)
      case BinaryMessage.Strict(data) => CborByteString.decode[T](data)
      case _                          => throw new RuntimeException("The expected message is not Strict")
    }
  }
}
