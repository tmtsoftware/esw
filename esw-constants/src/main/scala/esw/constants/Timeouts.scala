package esw.constants

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Timeouts {
  // agent
  val AgentSpawn: FiniteDuration = 15.seconds //todo: use with agent apis

  // sequence component
  val SequenceComponentStatus: FiniteDuration = 1.seconds
  val LoadScript: FiniteDuration              = 5.seconds
  val UnloadScript: FiniteDuration            = 3.seconds
  val Shutdown: FiniteDuration                = 4.seconds
  val RestartScript: FiniteDuration           = UnloadScript + LoadScript
  require(RestartScript <= 8.seconds)

  // sequencer
  val SequencerOperation: FiniteDuration     = 2.second
  val ScriptHandlerExecution: FiniteDuration = 5.second
  val GetSequenceComponent: FiniteDuration   = SequencerOperation

  // sequence Manager
  private val Processing: FiniteDuration   = 1.second // This includes time for processing other than 3rd party calls
  val GetAllRunningObsMode: FiniteDuration = 3.second

  val Configure: FiniteDuration = SequenceComponentStatus + LoadScript + Processing
  require(Configure <= 7.seconds)

  val Provision: FiniteDuration = Shutdown + AgentSpawn + Processing
  require(Provision <= 20.seconds)

  val StartSequencer: FiniteDuration = SequenceComponentStatus + LoadScript + Processing
  require(StartSequencer <= 7.seconds)

  val ShutdownSequencer: FiniteDuration = GetSequenceComponent + UnloadScript + Processing
  require(ShutdownSequencer <= 6.seconds)

  val RestartSequencer: FiniteDuration = GetSequenceComponent + RestartScript + Processing
  require(RestartSequencer <= 11.seconds)

  val ShutdownSequenceComponent: FiniteDuration = Shutdown

  val GetAllAgentStatus: FiniteDuration = SequenceComponentStatus + Processing
  require(GetAllAgentStatus <= 2.seconds)
}
