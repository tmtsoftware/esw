package esw.ocs.app.route

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.core.models.Id
import esw.http.core.BaseTestSuite
import esw.ocs.api.SequencerAdminApi
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerWebsocketRequest.QueryFinal
import esw.ocs.impl.SequencerCommandImpl
import io.bullet.borer.Decoder
import msocket.impl.Encoding
import msocket.impl.Encoding.{CborBinary, JsonText}
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.ws.WebsocketRouteFactory
import org.mockito.Mockito.when

import scala.concurrent.Future

class SequencerCommandWebsocketRouteTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerHttpCodecs
    with ClientHttpCodecs {

  override def encoding: Encoding[_] = JsonText

  private val sequencerCommandApi: SequencerCommandImpl = mock[SequencerCommandImpl]
  private val sequencerAdminApi: SequencerAdminApi      = mock[SequencerAdminApi]

  private def websocketHandlerFactory(encoding: Encoding[_]) =
    new SequencerWebsocketHandlerImpl(sequencerCommandApi, sequencerAdminApi, encoding)

  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")

  lazy val route: Route = new WebsocketRouteFactory("websocket-endpoint", websocketHandlerFactory).make()
  private val wsClient  = WSProbe()

  "SequencerRoutes" must {
    "return final submit response of sequence for QueryFinal request | ESW-101" in {
      val id                = Id("some")
      val completedResponse = Completed(id)
      when(sequencerCommandApi.queryFinal(id)).thenReturn(Future.successful(completedResponse))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(QueryFinal(id)))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[SubmitResponse](wsClient)
        response shouldEqual completedResponse

      }
    }
  }

  private def decodeMessage[T](wsClient: WSProbe)(implicit decoder: Decoder[T]): T = {
    wsClient.expectMessage() match {
      case TextMessage.Strict(text)   => JsonText.decode[T](text)
      case BinaryMessage.Strict(data) => CborBinary.decode[T](data)
      case _                          => throw new RuntimeException("The expected message is not Strict")
    }
  }
}
