package esw.ocs.app.route

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import esw.http.core.BaseTestSuite
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.SequencerAdminWebsocketRequest.QueryFinal
import esw.ocs.impl.SequencerAdminImpl
import msocket.impl.Encoding
import msocket.impl.post.ClientHttpCodecs
import msocket.impl.Encoding.JsonText

class SequencerAdminWebsocketRouteTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerAdminHttpCodecs
    with ClientHttpCodecs {

  override def encoding: Encoding[_] = JsonText

  private val sequencerAdmin: SequencerAdminImpl             = mock[SequencerAdminImpl]
  private val postHandler                                    = new PostHandlerImpl(sequencerAdmin)
  private def websocketHandlerFactory(encoding: Encoding[_]) = new WebsocketHandlerImpl(sequencerAdmin, encoding)
  private val route                                          = new SequencerAdminRoutes(postHandler, websocketHandlerFactory).route
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
