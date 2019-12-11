package esw.gateway.server

import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Started}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Subsystem.ESW
import csw.params.core.models.{ObsId, Prefix}
import esw.gateway.api.clients.ClientFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.{Gateway, EventServer}

class SequencerGatewayTest extends EswTestKit(Gateway, EventServer) with GatewayCodecs {
  private val subsystem     = ESW
  private val observingMode = "moonnight"

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnSequencerRef(subsystem, observingMode)
  }

  "SequencerApi" must {

    "handle submit, queryFinal commands | ESW-250" in {
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

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
