package esw.ocs.impl.extensions

import csw.command.client.messages.sequencer.SequencerMsg
import esw.ocs.api.models.StepStatus.{Finished, Pending}
import esw.ocs.api.models.{SequencerInsight, StepList}
import esw.ocs.impl.core.SequencerData
import esw.ocs.impl.messages.SequencerState

object SequencerInsightExtension {
  implicit class RichSequencerInsight(sequencerInsightType: SequencerInsight.type) {
    def apply(sequencerData: SequencerData, state: SequencerState[SequencerMsg]): SequencerInsight = {
      SequencerInsight(
        state.entryName,
        sequencerData.runId,
        sequencerData.stepList,
        getSequenceStatus(sequencerData.stepList)
      )
    }

    private def getSequenceStatus(stepListMayBe: Option[StepList]): Option[String] = {
      stepListMayBe.map(sl => {
        if (sl.isInFlight || sl.steps.exists(_.status == Pending)) "InProgress"
        else if (sl.isPaused) "Paused"
        else if (sl.isFinished)
          sl.steps
            .map(_.status)
            .collectFirst {
              case Finished.Failure(message) => s"Error - $message"
            }
            .getOrElse("Completed")
        else "Unknown"
      })
    }
  }
}
