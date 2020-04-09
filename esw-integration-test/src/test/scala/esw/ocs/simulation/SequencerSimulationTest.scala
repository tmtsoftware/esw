package esw.ocs.simulation

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.protocol.Ok
import esw.ocs.testkit.EswTestKit

class SequencerSimulationTest extends EswTestKit {

  override def beforeAll(): Unit = {
    super.beforeAll()
    spawnSequencerInSimulation(ESW, "moonnight")
  }

  "Sequencer in simulation" in {
    val sequencer = sequencerClient(ESW, "moonnight")

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val command2 = Setup(Prefix("esw.test"), CommandName("command-2"), None)
    val sequence = Sequence(command1, command2)

    sequencer.submitAndWait(sequence).futureValue shouldBe a[Completed]
    sequencer.goOffline().futureValue should ===(Ok)
  }
}
