package esw.commons.utils.config

import java.nio.file.Path

import com.typesafe.config.ConfigException
import csw.config.api.exceptions.FileNotFound
import csw.config.client.commons.ConfigUtils

import scala.concurrent.{ExecutionContext, Future}

class VersionManager(configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  private val scriptVersion = "scripts.version"
  def getScriptVersion(path: Path): Future[String] =
    configUtils
      .getConfig(path, isLocal = false)
      .map(config => config.getString(scriptVersion))
      .recover {
        case FileNotFound(msg)            => throw FetchingScriptVersionFailed(msg)
        case _: ConfigException.Missing   => throw FetchingScriptVersionFailed(s"$scriptVersion is not present")
        case _: ConfigException.WrongType => throw FetchingScriptVersionFailed(s"value of $scriptVersion is not string")
        case e                            => throw FetchingScriptVersionFailed(s"Failed to fetch script version: ${e.getMessage}")
      }
}
