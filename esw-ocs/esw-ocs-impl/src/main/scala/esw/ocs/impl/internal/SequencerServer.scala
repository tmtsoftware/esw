package esw.ocs.impl.internal

import akka.Done
import csw.location.models.AkkaLocation
import esw.ocs.api.responses.RegistrationError

import scala.concurrent.Future

trait SequencerServer {
  def start(): Either[RegistrationError, AkkaLocation]
  def shutDown(): Future[Done]
}

trait SequencerServerFactory {
  def make(sequencerId: String, observingMode: String, sequenceComponentName: Option[String]): SequencerServer
}
