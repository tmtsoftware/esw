package esw.ocs.api

import akka.stream.scaladsl.Source
import esw.ocs.api.models.SequencerInsight
import msocket.api.models.Subscription

trait SequencerAdminApi extends SequencerEditorApi with SequencerCommandApi {
  def getInsights: Source[SequencerInsight, Subscription]
}
