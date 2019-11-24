package esw.http.core.wiring

import com.typesafe.config.Config
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType}

class Settings(_port: Option[Int], _serviceName: Option[String], _config: Config, componentType: ComponentType) {
  lazy val config: Config                 = _config.getConfig("http-server")
  lazy val port: Int                      = _port.getOrElse(config.getInt("port"))
  lazy val serviceName: String            = _serviceName.getOrElse(config.getString("service-name"))
  lazy val httpConnection: HttpConnection = HttpConnection(ComponentId(serviceName, componentType))
}
