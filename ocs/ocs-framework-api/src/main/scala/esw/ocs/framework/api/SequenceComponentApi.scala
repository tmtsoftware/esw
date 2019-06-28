package esw.ocs.framework.api

import csw.location.api.models.AkkaLocation

import scala.concurrent.Future

trait SequenceComponentApi {
  // fixme: revisit return type
  def loadScript(sequencerId: String, observingMode: String): Future[Either[AkkaLocation, AkkaLocation]]
  def stopScript: Future[Unit]
  def getStatus: Future[Option[AkkaLocation]]
}
