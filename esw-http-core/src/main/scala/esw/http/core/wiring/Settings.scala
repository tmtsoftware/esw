package esw.http.core.wiring

import com.typesafe.config.{Config}
import csw.location.model.scaladsl.Connection.HttpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

class Settings(_port: Option[Int], _config: Config) {
  lazy val config: Config      = _config.getConfig("http-server")
  lazy val port: Int           = _port.getOrElse(config.getInt("port"))
  lazy val serviceName: String = config.getString("service-name")
  lazy val httpConnection      = HttpConnection(ComponentId(serviceName, ComponentType.Service))
}
