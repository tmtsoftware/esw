package esw.sm.api.models

import esw.ocs.api.models.SequencerId

case class Sequencers(sequencerIds: List[SequencerId])
object Sequencers {
  def apply(sequencerIds: SequencerId*): Sequencers = new Sequencers(sequencerIds.toList)
}
