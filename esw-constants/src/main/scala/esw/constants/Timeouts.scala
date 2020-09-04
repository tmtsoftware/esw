package esw.constants

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Timeouts {
  // agent client apis
  val AgentSpawn: FiniteDuration = 20.seconds // 7
  val AgentKill: FiniteDuration  = 5.seconds // 1

  // admin
  val GetLogMetadata: FiniteDuration = 2.seconds // 2

  // sequence component apis
  val Status: FiniteDuration        = 1.seconds
  val LoadScript: FiniteDuration    = 5.seconds
  val UnloadScript: FiniteDuration  = 3.seconds
  val Shutdown: FiniteDuration      = 4.seconds
  val RestartScript: FiniteDuration = UnloadScript + LoadScript
  require(RestartScript < 10.seconds)

  // sequencer apis
  val SubmitAndWait: FiniteDuration        = 1.second
  val IsOnline: FiniteDuration             = 1.second
  val GetSequenceComponent: FiniteDuration = 1.second
  val Add: FiniteDuration                  = 1.second
  val LoadSequence: FiniteDuration         = 1.second
  val StartSequence: FiniteDuration        = 1.second
  val GetSequence: FiniteDuration          = 1.second
  val QueryFinal: FiniteDuration           = 1.second //external timeout
  val Query: FiniteDuration                = 1.second

  // Executes handlers
  val AbortSequence: FiniteDuration  = 1.second
  val StopSequence: FiniteDuration   = 1.second
  val GoOnline: FiniteDuration       = 2.second
  val GoOffline: FiniteDuration      = 2.second
  val DiagnosticMode: FiniteDuration = 1.second
  val OperationsMode: FiniteDuration = 1.second
  val SubmitSequence: FiniteDuration = 1.second

  val SequenceOperation: FiniteDuration = 2.second // better name
  val HandlerExecution: FiniteDuration  = 5.second

}
