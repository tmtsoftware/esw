package esw.sm.api.models

import esw.ocs.api.models.SequencerId

case class SequencerIds(sequencerIds: List[SequencerId])
object SequencerIds {
  def apply(sequencerIds: SequencerId*): SequencerIds = new SequencerIds(sequencerIds.toList)
}
