package esw.ocs.api.models

import csw.params.core.models.Id

case class SequencerInsight(
    stepList: Option[StepList],
    runId: Option[Id],
    sequencerState: String
)
