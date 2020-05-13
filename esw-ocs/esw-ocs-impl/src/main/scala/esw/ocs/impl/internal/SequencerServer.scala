package esw.ocs.impl.internal

import akka.Done
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.StartSequencerError

// Note: The APIs in this service are blocking. SequenceComponentBehavior consumes this api and since
// these operations are not performed very often they could be blocked. Blocking here
// significantly simplifies the design of SequenceComponentBehavior.
trait SequencerServer {
  def start(): Either[StartSequencerError, AkkaLocation]
  def shutDown(): Done
}

trait SequencerServerFactory {
  def make(subsystem: Subsystem, observingMode: String, sequenceComponentLocation: AkkaLocation): SequencerServer
}
