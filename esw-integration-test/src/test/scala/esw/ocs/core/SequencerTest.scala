package esw.ocs.core

import akka.Done
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.Step
import esw.ocs.internal.SequencerWiring

class SequencerTest extends ScalaTestFrameworkTestKit with BaseTestSuite {
  private var wiring: SequencerWiring = _

  override protected def beforeEach(): Unit = {
    wiring = new SequencerWiring("testSequencerId1", "testObservingMode1")
    wiring.start()
  }

  override protected def afterEach(): Unit = {
    wiring.shutDown()
  }

  "Sequencer" must {
    "process a given sequence | ESW-145" in {
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(command3)

      val processSeqResponse = wiring.sequencer.processSequence(sequence)

      processSeqResponse.rightValue should ===(Completed(sequence.runId))

      wiring.sequencer.getSequence.futureValue.steps should ===(
        List(Step(command3, Finished.Success(Completed(command3.runId)), hasBreakpoint = false))
      )
    }

    "process sequence and execute commands that are added later | ESW-145" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Setup(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)

      val sequence = Sequence(command1, command2)

      val processSeqResponse = wiring.sequencer.processSequence(sequence)

      wiring.sequenceEditorClient.add(List(command3)).futureValue.response.rightValue should ===(Done)

      processSeqResponse.rightValue should ===(Completed(sequence.runId))

      wiring.sequencer.getSequence.futureValue.steps should ===(
        List(
          Step(command1, Finished.Success(Completed(command1.runId)), hasBreakpoint = false),
          Step(command2, Finished.Success(Completed(command2.runId)), hasBreakpoint = false),
          Step(command3, Finished.Success(Completed(command3.runId)), hasBreakpoint = false)
        )
      )
    }
  }
}
