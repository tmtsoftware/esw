package esw.ocs.api.models

import csw.params.core.models.Id

case class SequencerInsight(
    sequencerState: String,
    runId: Option[Id],
    stepList: Option[StepList],
    sequenceStatus: Option[String]
)
