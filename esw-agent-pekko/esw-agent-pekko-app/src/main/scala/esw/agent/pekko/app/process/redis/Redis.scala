package esw.agent.pekko.app.process.redis

import esw.agent.pekko.app.process.ProcessUtils

object Redis {
  private val redisServer = "redis-server"

  // todo: add version check
  def server: String = {
    if (ProcessUtils.isInstalled(redisServer)) redisServer
    else throw new RuntimeException(s"$redisServer is not installed")
  }
}
