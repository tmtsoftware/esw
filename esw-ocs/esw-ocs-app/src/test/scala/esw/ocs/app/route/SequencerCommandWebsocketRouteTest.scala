package esw.ocs.app.route

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import esw.http.core.BaseTestSuite
import esw.ocs.api.codecs.SequencerHttpCodecs
import esw.ocs.api.protocol.SequencerCommandWebsocketRequest.QueryFinal
import esw.ocs.impl.{SequencerAdminImpl, SequencerCommandImpl}
import msocket.impl.Encoding
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.Encoding.JsonText

class SequencerCommandWebsocketRouteTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerHttpCodecs
    with ClientHttpCodecs {

  override def encoding: Encoding[_] = JsonText

  private val sequencerCommand: SequencerCommandImpl         = mock[SequencerCommandImpl]
  private val postHandler                                    = new CommandPostHandlerImpl(sequencerCommand)
  private def websocketHandlerFactory(encoding: Encoding[_]) = new CommandWebsocketHandlerImpl(sequencerCommand, encoding)
  private val route                                          = new SequencerCommandRoutes(postHandler, websocketHandlerFactory).route
  private val wsClient                                       = WSProbe()

  "SequencerRoutes" must {
    "return final submit response of sequence for QueryFinal request | ESW-101" in {
      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(QueryFinal))
        isWebSocketUpgrade shouldBe true
      }
    }
  }

}
