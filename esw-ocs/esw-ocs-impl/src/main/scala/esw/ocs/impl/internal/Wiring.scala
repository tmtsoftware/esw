package esw.ocs.impl.internal

import akka.Done
import csw.location.models.AkkaLocation
import esw.ocs.api.models.responses.RegistrationError

import scala.concurrent.Future

trait Wiring {

  def start(): Either[RegistrationError, AkkaLocation]

  def shutDown(): Future[Done]
}
