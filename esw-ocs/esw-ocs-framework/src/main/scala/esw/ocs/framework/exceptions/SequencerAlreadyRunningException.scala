package esw.ocs.framework.exceptions

// fixme: parameterize exception with sequence component name
//  do not use singleton objects for exceptions, does not carry true information of exceptions like line no, stackstraces etc.
object SequencerAlreadyRunningException extends RuntimeException("Sequence Component is already running a sequencer")
