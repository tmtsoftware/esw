package esw.commons.utils.config

import java.nio.file.Path

import com.typesafe.config.ConfigException
import csw.config.api.exceptions.FileNotFound
import csw.config.client.commons.ConfigUtils

import scala.concurrent.{ExecutionContext, Future}

/**
 * This utility class is used to manage versions of various TMT repositories.
 * @param versionConfPath - path of the version conf in config service
 * @param configUtils - an instance of configUtils
 * @param ec - an implicit ExecutionContext
 */
class VersionManager(versionConfPath: Path, configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  lazy val eswVersion: Future[String]  = getVersionFor(versionConfPath, "esw")
  def getScriptVersion: Future[String] = getVersionFor(versionConfPath, "scripts")

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
