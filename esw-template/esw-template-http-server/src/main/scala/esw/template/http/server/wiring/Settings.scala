package esw.template.http.server.wiring

import com.typesafe.config.{Config, ConfigFactory}
import csw.location.model.scaladsl.Connection.HttpConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}

class Settings(_port: Option[Int]) {
  lazy val config: Config     = ConfigFactory.load().getConfig("http-server")
  lazy val port: Int          = _port.getOrElse(config.getInt("port"))
  lazy val connection: String = config.getString("connection-name")
  lazy val httpConnection     = HttpConnection(ComponentId(connection, ComponentType.Service))
}
