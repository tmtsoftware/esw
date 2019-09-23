package esw.ocs.api

import scala.concurrent.Future

trait SequencerAdminFactoryApi {
  def make(sequencerId: String, observingMode: String): Future[SequencerAdminApi]
}
