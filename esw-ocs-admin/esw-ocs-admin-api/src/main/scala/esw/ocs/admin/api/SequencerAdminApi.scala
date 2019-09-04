package esw.ocs.admin.api

import esw.ocs.api.models.StepList

import scala.concurrent.Future

trait SequencerAdminApi {
  def getSequence(sequencerName: String): Future[Option[StepList]]
}
