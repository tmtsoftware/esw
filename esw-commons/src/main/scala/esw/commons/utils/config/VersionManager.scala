package esw.commons.utils.config

import java.nio.file.Path

import com.typesafe.config.ConfigException
import csw.config.api.exceptions.FileNotFound
import csw.config.client.commons.ConfigUtils

import scala.concurrent.{ExecutionContext, Future}

class VersionManager(versionConfPath: Path, configUtils: ConfigUtils)(implicit ec: ExecutionContext) {

  private val scriptsKey              = "scripts"
  lazy val eswVersion: Future[String] = getVersionFor(versionConfPath, "esw")

  def getScriptVersion: Future[String] = getVersionFor(versionConfPath, scriptsKey)

  private def getVersionFor(path: Path, key: String): Future[String] =
    configUtils
      .getConfig(path, isLocal = false)
      .map(config => config.getString(key))
      .recover {
        case FileNotFound(msg)            => throw FetchingScriptVersionFailed(msg)
        case _: ConfigException.Missing   => throw FetchingScriptVersionFailed(s"$key is not present")
        case _: ConfigException.WrongType => throw FetchingScriptVersionFailed(s"value of $key is not string")
        case e                            => throw FetchingScriptVersionFailed(s"Failed to fetch $key version: ${e.getMessage}")
      }
}
