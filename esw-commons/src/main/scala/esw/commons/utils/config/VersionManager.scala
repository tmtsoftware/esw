package esw.commons.utils.config

import com.typesafe.config.ConfigException
import csw.config.api.exceptions.FileNotFound
import csw.config.client.commons.ConfigUtils

import java.nio.file.Path
import scala.concurrent.{ExecutionContext, Future}

class VersionManager(configUtils: ConfigUtils)(implicit ec: ExecutionContext) {
  private val scriptVersion = "scripts.version"
  def getScriptVersion(path: Path): Future[String] =
    configUtils
      .getConfig(path, isLocal = false)
      .map(config => config.getString(scriptVersion))
      .recover {
        case FileNotFound(msg)            => throw ScriptVersionConfException(msg)
        case _: ConfigException.Missing   => throw ScriptVersionConfException(s"$scriptVersion is not present")
        case _: ConfigException.WrongType => throw ScriptVersionConfException(s"value of $scriptVersion is not string")
        case ex                           => throw ScriptVersionConfException(ex.getMessage)
      }
}
