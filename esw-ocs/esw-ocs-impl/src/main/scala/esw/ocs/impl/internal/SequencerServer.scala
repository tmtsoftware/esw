package esw.ocs.impl.internal

import akka.Done
import csw.location.models.AkkaLocation
import csw.params.core.models.Subsystem
import esw.ocs.api.protocol.ScriptError

// Note: The APIs in this service are blocking. SequenceComponentBehavior consumes this api and since
// these operations are not performed very often they could be blocked. Blocking here
// significantly simplifies the design of SequenceComponentBehavior.
trait SequencerServer {
  def start(): Either[ScriptError, AkkaLocation]
  def shutDown(): Done
}

trait SequencerServerFactory {
  def make(subsystem: Subsystem, observingMode: String, sequenceComponentLocation: AkkaLocation): SequencerServer
}
