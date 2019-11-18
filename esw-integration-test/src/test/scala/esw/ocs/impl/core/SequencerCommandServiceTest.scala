package esw.ocs.impl.core

import csw.command.api.scaladsl.SequencerCommandService
import csw.command.client.SequencerCommandServiceFactory
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.testkit.EswTestKit

class SequencerCommandServiceTest extends EswTestKit {

  "should submit and process sequence | ESW-190, ESW-148" in {
    val sequencerLocation = spawnSequencer("esw", "darknight").rightValue

    val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
    val sequence = Sequence(command1)

    val sequencerCommandService: SequencerCommandService = SequencerCommandServiceFactory.make(sequencerLocation)
    sequencerCommandService.submitAndWait(sequence).futureValue should ===(Completed(sequence.runId))
  }
}
