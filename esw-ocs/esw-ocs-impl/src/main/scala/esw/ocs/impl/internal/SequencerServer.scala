package esw.ocs.impl.internal

import org.apache.pekko.Done
import csw.location.api.models.PekkoLocation
import csw.prefix.models.Prefix
import esw.ocs.api.protocol.ScriptError

// Note: The APIs in this service are blocking. SequenceComponentBehavior consumes this api and since
// these operations are not performed very often they could be blocked. Blocking here
// significantly simplifies the design of SequenceComponentBehavior.
trait SequencerServer {
  def start(): Either[ScriptError, PekkoLocation]
  def shutDown(): Done
}

trait SequencerServerFactory {
  // Note: See SequencerApp.sequenceComponentWiring() for SAM (Single Abstract Method) implementation
  def make(sequencerPrefix: Prefix, sequenceComponentPrefix: Prefix): SequencerServer
}
