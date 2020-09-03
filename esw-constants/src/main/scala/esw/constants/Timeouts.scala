package esw.constants

import scala.concurrent.duration.{DurationInt, FiniteDuration}

object Timeouts {
  // agent client apis
  val AgentSpawn: FiniteDuration = 7.seconds
  val AgentKill: FiniteDuration  = 1.seconds

  // admin
  val GetLogMetadata: FiniteDuration = 2.seconds

  // sequence component apis
  val StatusTimeout: FiniteDuration        = 1.seconds
  val LoadScriptTimeout: FiniteDuration    = 5.seconds
  val UnloadScriptTimeout: FiniteDuration  = 3.seconds
  val ShutdownTimeout: FiniteDuration      = 4.seconds
  val RestartScriptTimeout: FiniteDuration = UnloadScriptTimeout + LoadScriptTimeout
  require(RestartScriptTimeout < 10.seconds)
}
