package esw.gateway.impl

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory

class LoggerCache {
  private val loggerCache: LoadingCache[String, Logger] = Caffeine
    .newBuilder()
    .maximumSize(2048)
    .build(key => new LoggerFactory(key.toLowerCase).getLogger)

  def get(componentName: String): Logger = loggerCache.get(componentName)
}
