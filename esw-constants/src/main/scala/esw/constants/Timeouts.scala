package esw.constants

import scala.concurrent.duration.{DurationInt, FiniteDuration}

/**
 * This file is created with intention to keep all timeouts used in esw at one place.
 * Timeouts are grouped in separately according to their usages. For example,
 *
 * [[AgentTimeouts]] contain timeouts that are used by the agent APIs.
 *
 * [[CommonTimeouts]] contain timeouts that are commonly used more than one services.
 *
 * Some timeouts are composed from multiple other timeouts. For example,
 * {{{val timeout1: FiniteDuration = timeout2 + timeout3}}}
 * In such cases we have added a requirement that puts an upper bound on the resulting timeout
 * so that it does not exceed some fixed amount of time when the underlying timeouts change:
 * {{{require(timeout1 <= x.seconds)}}}
 */

object CommonTimeouts {
  val Wiring: FiniteDuration =
    10.seconds // Generic timeout to be used in apps and wiring for starting/stopping actor systems, http servers etc.
  val ResolveLocation: FiniteDuration = 3.seconds // Generic timeout for resolving a location using location service.
  val FetchConfig: FiniteDuration     = 2.seconds // Generic timeout for fetching a config file from config service
}

object AgentTimeouts {
  val DurationToWaitForComponentRegistration: FiniteDuration = 18.seconds
  val SpawnComponent: FiniteDuration                         = 20.seconds
  require(
    DurationToWaitForComponentRegistration <= SpawnComponent,
    "SpawnComponent composes over DurationToWaitForComponentRegistration. DurationToWaitForComponentRegistration should be lesser"
  )
  val KillComponent: FiniteDuration = 3.seconds
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
  private val Processing: FiniteDuration = 1.second // This includes time for processing other than 3rd party calls
  val GetObsModesDetails: FiniteDuration = 1.seconds
  require(GetObsModesDetails <= 2.seconds, "max timeout violated for GetObsModesDetails")

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

  val GetResources: FiniteDuration = SequenceComponentTimeouts.Status
  require(GetResources <= 2.seconds, "max timeout violated for GetResources")
}

object AdminTimeouts {
  val GetLogMetadata: FiniteDuration = 2.seconds
}
