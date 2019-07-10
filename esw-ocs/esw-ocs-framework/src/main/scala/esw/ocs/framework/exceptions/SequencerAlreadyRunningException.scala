package esw.ocs.framework.exceptions

// fixme: parameterize exception with sequence component name
object SequencerAlreadyRunningException extends RuntimeException("Sequence Component is already running a sequencer")
