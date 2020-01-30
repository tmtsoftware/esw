package esw.http.core.wiring

import com.typesafe.config.Config
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}
import csw.prefix.models.Prefix

class Settings(_port: Option[Int], _prefix: Option[Prefix], _config: Config, componentType: ComponentType) {
  lazy val config: Config                 = _config.getConfig("http-server")
  lazy val port: Int                      = _port.getOrElse(config.getInt("port"))
  lazy val prefix: Prefix                 = _prefix.getOrElse(Prefix(config.getString("prefix")))
  lazy val httpConnection: HttpConnection = HttpConnection(ComponentId(prefix, componentType))
}
