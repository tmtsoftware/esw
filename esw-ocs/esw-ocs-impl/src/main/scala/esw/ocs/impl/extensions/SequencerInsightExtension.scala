package esw.ocs.impl.extensions

import esw.ocs.api.models.SequencerInsight
import esw.ocs.impl.core.SequencerData

import scala.util.Random

object SequencerInsightExtension {
  implicit class RichSequencerInsight(sequencerInsightType: SequencerInsight.type) {
    def apply(sequencerState: SequencerData): SequencerInsight = {
      SequencerInsight(Random.nextInt)
    }
  }
}
