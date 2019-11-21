package esw.ocs.impl.core

import akka.actor.testkit.typed.scaladsl.TestProbe
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.api.protocol.{OkOrUnhandledResponse, SequencerSubmitResponse, SubmitResult, Unhandled}
import esw.ocs.impl.messages.SequencerMessages.{LoadSequence, SubmitSequence}
import esw.ocs.testkit.EswTestKit

class SequencerBehaviorIntegrationTest extends EswTestKit {
  private val ocsPackageId     = "esw"
  private val ocsObservingMode = "darknight"

  "Sequencer" must {
    "not receive sequence when already processing a sequence | ESW-145" in {
      val command                   = Setup(Prefix("TCS.test"), CommandName("test-sequencer-hierarchy"), None)
      val submitResponseProbe       = TestProbe[SequencerSubmitResponse]
      val loadSequenceResponseProbe = TestProbe[OkOrUnhandledResponse]
      val sequence                  = Sequence(Seq(command))
      val ocsSequencer              = spawnSequencerRef(ocsPackageId, ocsObservingMode)

      ocsSequencer ! SubmitSequence(sequence, submitResponseProbe.ref)
      ocsSequencer ! LoadSequence(sequence, loadSequenceResponseProbe.ref)

      // response received by irisSequencer
      submitResponseProbe.expectMessageType[SubmitResult]
      loadSequenceResponseProbe.expectMessage(Unhandled("InProgress", "LoadSequence"))
    }
  }
}
