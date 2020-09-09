package esw.constants

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object CommonTimeouts {
  val Wiring: FiniteDuration          = 10.seconds
  val ResolveLocation: FiniteDuration = 3.seconds
}

object AgentTimeouts {
  val SpawnComponent: FiniteDuration = 20.seconds
  val KillComponent: FiniteDuration  = 3.seconds
}

object SequenceComponentTimeouts {
  private val Processing: FiniteDuration = 1.second                                             // This includes time for processing other than 3rd party calls
  val Status: FiniteDuration             = 1.second
  val LoadScript: FiniteDuration         = SequencerTimeouts.ScriptHandlerExecution
  val UnloadScript: FiniteDuration       = SequencerTimeouts.ScriptHandlerExecution + 2.seconds // shutdown redis client
  val Shutdown: FiniteDuration           = UnloadScript + Processing
  require(Shutdown <= 8.seconds, "max timeout violated for Shutdown")
  val RestartScript: FiniteDuration = UnloadScript + LoadScript
  require(RestartScript <= 12.seconds, "max timeout violated for RestartScript")
}

object SequencerTimeouts {
  val LongTimeout: FiniteDuration            = 10.hours
  val SequencerOperation: FiniteDuration     = 2.seconds
  val ScriptHandlerExecution: FiniteDuration = 5.seconds
  val GetSequenceComponent: FiniteDuration   = SequencerOperation
}

object SequenceManagerTimeouts {
  private val Processing: FiniteDuration   = 1.second // This includes time for processing other than 3rd party calls
  val GetAllRunningObsMode: FiniteDuration = 3.seconds

  val Configure: FiniteDuration = SequenceComponentTimeouts.Status + SequenceComponentTimeouts.LoadScript + Processing
  require(Configure <= 7.seconds, "max timeout violated for Configure")

  val Provision: FiniteDuration = SequenceComponentTimeouts.Shutdown + AgentTimeouts.SpawnComponent + Processing
  require(Provision <= 29.seconds, "max timeout violated for Provision")

  val StartSequencer: FiniteDuration = SequenceComponentTimeouts.Status + SequenceComponentTimeouts.LoadScript + Processing
  require(StartSequencer <= 7.seconds, "max timeout violated for StartSequencer")

  val ShutdownSequencer: FiniteDuration =
    SequencerTimeouts.GetSequenceComponent + SequenceComponentTimeouts.UnloadScript + Processing
  require(ShutdownSequencer <= 10.seconds, "max timeout violated for ShutdownSequencer")

  val RestartSequencer: FiniteDuration =
    SequencerTimeouts.GetSequenceComponent + SequenceComponentTimeouts.RestartScript + Processing
  require(RestartSequencer <= 15.seconds, "max timeout violated for RestartSequencer")

  val ShutdownSequenceComponent: FiniteDuration = SequenceComponentTimeouts.Shutdown

  val GetAllAgentStatus: FiniteDuration = SequenceComponentTimeouts.Status + Processing
  require(GetAllAgentStatus <= 2.seconds, "max timeout violated for GetAllAgentStatus")
}

object AdminTimeouts {
  val GetLogMetadata: FiniteDuration = 2.seconds
}
