package esw.agent.akka.app.process.redis

import esw.agent.akka.app.process.ProcessUtils

object Redis {
  private val redisServer = "redis-server"

  // todo: add version check
  def server: String = {
    if (ProcessUtils.isInstalled(redisServer)) redisServer
    else throw new RuntimeException(s"$redisServer is not installed")
  }
}
