package esw.gateway.server

import akka.actor.CoordinatedShutdown.UnknownReason
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Started}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Subsystem.ESW
import csw.params.core.models.{ObsId, Prefix}
import esw.gateway.api.clients.ClientFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.gateway.api.protocol.{PostRequest, WebsocketRequest}
import esw.ocs.testkit.EswTestKit
import msocket.api.Transport
import msocket.impl.Encoding.JsonText
import msocket.impl.post.HttpPostTransport
import msocket.impl.ws.WebsocketTransport

class SequencerGatewayTest extends EswTestKit with GatewayCodecs {
  private val port: Int                    = 6490
  private val subsystem                    = ESW
  private val observingMode                = "moonnight"
  private val gatewayWiring: GatewayWiring = new GatewayWiring(Some(port))

  override def beforeAll(): Unit = {
    super.beforeAll()
    gatewayWiring.httpService.registeredLazyBinding.futureValue
    spawnSequencerRef(subsystem, observingMode)
  }

  override def afterAll(): Unit = {
    gatewayWiring.httpService.shutdown(UnknownReason).futureValue
    super.afterAll()
  }

  "SequencerApi" must {

    "handle submit, queryFinal commands | ESW-250" in {
      val postClient: Transport[PostRequest] =
        new HttpPostTransport(s"http://localhost:$port/post-endpoint", JsonText, () => None)
      val websocketClient: Transport[WebsocketRequest] =
        new WebsocketTransport(s"ws://localhost:$port/websocket-endpoint", JsonText)
      val clientFactory = new ClientFactory(postClient, websocketClient)

      val sequence    = Sequence(Setup(Prefix("esw.test"), CommandName("command-2"), Some(ObsId("obsId"))))
      val componentId = ComponentId(Prefix(s"$subsystem.$observingMode"), ComponentType.Sequencer)

      val sequencer = clientFactory.sequencer(componentId)

      //submit sequence
      val submitResponse = sequencer.submit(sequence).futureValue
      submitResponse shouldBe a[Started]

      //queryFinal
      sequencer.queryFinal(submitResponse.runId).futureValue should ===(Completed(submitResponse.runId))
    }
  }
}
