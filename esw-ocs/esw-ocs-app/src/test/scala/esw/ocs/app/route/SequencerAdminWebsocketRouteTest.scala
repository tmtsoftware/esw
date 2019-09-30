package esw.ocs.app.route

import akka.http.scaladsl.testkit.{ScalatestRouteTest, WSProbe}
import esw.http.core.BaseTestSuite
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.protocol.SequencerAdminWebsocketRequest.QueryFinal
import esw.ocs.impl.SequencerAdminImpl
import mscoket.impl.HttpCodecs
import mscoket.impl.ws.Encoding.JsonText

class SequencerAdminWebsocketRouteTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequencerAdminHttpCodecs
    with HttpCodecs {

  private val sequencerAdmin: SequencerAdminImpl = mock[SequencerAdminImpl]
  private val postHandler                        = new PostHandlerImpl(sequencerAdmin)
  private val websocketHandler                   = new WebsocketHandlerImpl(sequencerAdmin)
  private val route                              = new SequencerAdminRoutes(postHandler, websocketHandler).route
  private val wsClient                           = WSProbe()

  "SequencerRoutes" must {
    "return final submit response of sequence for QueryFinal request | ESW-101" in {
      WS("/websocket-endpoint", wsClient.flow) ~> route ~> check {
        wsClient.sendMessage(JsonText.strictMessage(QueryFinal))
        isWebSocketUpgrade shouldBe true
      }
    }
  }

}
