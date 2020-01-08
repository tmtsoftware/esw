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
import esw.ocs.api.{BaseTestSuite, SequencerApi}
import io.bullet.borer.Decoder
import msocket.api.Encoding
import msocket.api.Encoding.JsonText
import msocket.impl.CborByteString
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.ws.EncodingExtensions.EncodingForMessage
import msocket.impl.ws.WebsocketRouteFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerCommandWebsocketRouteTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerHttpCodecs
    with ClientHttpCodecs {

  override def encoding: Encoding[_] = JsonText

  private val sequencer: SequencerApi = mock[SequencerApi]

  private def websocketHandlerFactory(encoding: Encoding[_]) = new SequencerWebsocketHandler(sequencer, encoding)

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")

  lazy val route: Route =
    new WebsocketRouteFactory[SequencerWebsocketRequest]("websocket-endpoint", websocketHandlerFactory).make()

  private val wsClient = WSProbe()

  "SequencerRoutes" must {
    "return final submit response of sequence for QueryFinal request | ESW-101" in {
      val id                        = Id("some")
      implicit val timeout: Timeout = Timeout(10.seconds)
      val completedResponse         = Completed(id)
      when(sequencer.queryFinal(id)).thenReturn(Future.successful(completedResponse))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(QueryFinal(id, timeout): SequencerWebsocketRequest))
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
