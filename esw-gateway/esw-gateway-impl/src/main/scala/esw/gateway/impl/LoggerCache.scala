package esw.gateway.impl

import com.github.benmanes.caffeine.cache.{Caffeine, LoadingCache}
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.prefix.models.Prefix

class LoggerCache {
  private val loggerCache: LoadingCache[Prefix, Logger] = Caffeine
    .newBuilder()
    .maximumSize(2048)
    .build(key => new LoggerFactory(key).getLogger)

  // loggerCache.get will enter LoggerFactory against key if not present
  def get(prefix: Prefix): Logger = loggerCache.get(prefix)
}
