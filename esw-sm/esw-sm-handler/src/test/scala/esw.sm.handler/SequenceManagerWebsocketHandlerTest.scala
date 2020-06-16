package esw.sm.handler

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.model.ws.{BinaryMessage, TextMessage}
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import akka.util.Timeout
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.commons.BaseTestSuite
import esw.sm.api.SequenceManagerApi
import esw.sm.api.codecs.SequenceManagerHttpCodec
import esw.sm.api.protocol.SequenceManagerWebsocketRequest.Configure
import esw.sm.api.protocol.{ConfigureResponse, SequenceManagerWebsocketRequest}
import io.bullet.borer.Decoder
import msocket.api.ContentEncoding.JsonText
import msocket.api.ContentType
import msocket.impl.CborByteString
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.ws.WebsocketExtensions.WebsocketEncoding
import msocket.impl.ws.WebsocketRouteFactory
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future

class SequenceManagerWebsocketHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequenceManagerHttpCodec
    with ClientHttpCodecs {

  override def clientContentType: ContentType = ContentType.Json

  private val obsMode                                = "IRIS_darknight"
  private val componentId                            = ComponentId(Prefix(ESW, obsMode), ComponentType.Sequencer)
  private val sequenceManagerApi: SequenceManagerApi = mock[SequenceManagerApi]
  private def websocketHandlerFactory(contentType: ContentType) =
    new SequenceManagerWebsocketHandler(sequenceManagerApi, contentType)
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "test-system")
  private lazy val route: Route =
    new WebsocketRouteFactory[SequenceManagerWebsocketRequest]("websocket-endpoint", websocketHandlerFactory).make()
  private val wsClient = WSProbe()

  "SequenceManagerWebsocketHandler" must {
    "return configure success response for configure request | ESW-171" in {
      implicit val timeout: Timeout = Timeout(10.seconds)
      when(sequenceManagerApi.configure(obsMode)).thenReturn(Future.successful(ConfigureResponse.Success(componentId)))

      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(ContentType.Json.strictMessage(Configure(obsMode, timeout): SequenceManagerWebsocketRequest))
        isWebSocketUpgrade shouldBe true

        val response = decodeMessage[ConfigureResponse](wsClient)
        response shouldEqual ConfigureResponse.Success(componentId)
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
