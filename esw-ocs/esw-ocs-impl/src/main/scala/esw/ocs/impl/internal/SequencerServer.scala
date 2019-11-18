package esw.ocs.impl.internal

import akka.Done
import csw.location.models.AkkaLocation
import esw.ocs.api.protocol.LoadScriptError

import scala.concurrent.Future

// Note: The APIs in this service are blocking. SequenceComponentBehavior consumes this api and since
// these operations are not performed very often they could be blocked. Blocking here
// significantly simplifies the design of SequenceComponentBehavior.
trait SequencerServer {
  def start(): Either[LoadScriptError, AkkaLocation]
  def shutDown(): Done
}

trait SequencerServerFactory {
  def make(packageId: String, observingMode: String, sequenceComponentName: Option[String]): SequencerServer
}
