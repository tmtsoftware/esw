package esw.gateway.server

import csw.location.api.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{Completed, Error, Started}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.testkit.scaladsl.CSWService.EventServer
import esw.gateway.api.clients.ClientFactory
import esw.gateway.api.codecs.GatewayCodecs
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.EswTestKit
import esw.ocs.testkit.Service.Gateway

class SequencerContractTest extends EswTestKit(Gateway, EventServer) with GatewayCodecs {
  private val subsystem = ESW
  private val obsMode   = ObsMode("MoonNight") // TestScript2.kts

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnSequencerRef(subsystem, obsMode)
  }

  "SequencerApi" must {

    "handle submit, queryFinal commands | ESW-250, ESW-98" in {
      // gatewayPostClient and gatewayWsClient requires gateway location which is resolved using Location Service in EswTestKit
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

      val sequence    = Sequence(Setup(Prefix("esw.test"), CommandName("command-2"), Some(ObsId("2020A-001-123"))))
      val componentId = ComponentId(Prefix(s"$subsystem.${obsMode.name}"), ComponentType.Sequencer)

      val sequencer = clientFactory.sequencer(componentId)

      //submit sequence
      val submitResponse = sequencer.submit(sequence).futureValue
      submitResponse shouldBe a[Started]

      //queryFinal
      sequencer.queryFinal(submitResponse.runId).futureValue should ===(Completed(submitResponse.runId))
    }

    "handle submit, queryFinal commands with error | ESW-250" in {
      val clientFactory = new ClientFactory(gatewayPostClient, gatewayWsClient)

      val failCmdName = CommandName("fail-command")
      val sequence    = Sequence(Setup(Prefix("esw.test"), failCmdName, Some(ObsId("2020A-001-123"))))
      val componentId = ComponentId(Prefix(s"$subsystem.${obsMode.name}"), ComponentType.Sequencer)

      val sequencer = clientFactory.sequencer(componentId)

      //submit sequence
      val submitResponse = sequencer.submit(sequence).futureValue
      submitResponse shouldBe a[Started]

      //queryFinal
      val response = sequencer.queryFinal(submitResponse.runId).futureValue.asInstanceOf[Error]
      response.runId should ===(submitResponse.runId)
      response.message should fullyMatch regex s"StepId: .*, CommandName: ${failCmdName.name}, reason: fail-command"
    }
  }
}
