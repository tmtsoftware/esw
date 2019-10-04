package esw.ocs.impl.internal

import akka.Done
import csw.location.models.AkkaLocation
import esw.ocs.api.protocol.LoadScriptError

import scala.concurrent.Future

trait SequencerServer {
  def start(): Either[LoadScriptError, AkkaLocation]
  def shutDown(): Future[Done]
}

trait SequencerServerFactory {
  def make(packageId: String, observingMode: String, sequenceComponentName: Option[String]): SequencerServer
}
