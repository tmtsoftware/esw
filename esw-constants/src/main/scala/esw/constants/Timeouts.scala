package esw.constants

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Timeouts {
  // sequence component
  val SequenceComponentStatus: FiniteDuration = 1.seconds
  val LoadScript: FiniteDuration              = 5.seconds
  val UnloadScript: FiniteDuration            = 3.seconds
  val Shutdown: FiniteDuration                = 4.seconds
  val RestartScript: FiniteDuration           = UnloadScript + LoadScript
  require(RestartScript <= 8.seconds)

  // sequencer apis
  val SequencerOperation: FiniteDuration     = 2.second
  val ScriptHandlerExecution: FiniteDuration = 5.second
}
