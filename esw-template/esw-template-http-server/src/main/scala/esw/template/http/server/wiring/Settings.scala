package esw.template.http.server.wiring

import com.typesafe.config.{Config, ConfigFactory}
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType}

import scala.compat.java8.DurationConverters.DurationOps
import scala.concurrent.duration.FiniteDuration

class Settings(_port: Option[Int]) {
  lazy val config: Config                       = ConfigFactory.load().getConfig("http-server")
  lazy val port: Int                            = _port.getOrElse(config.getInt("port"))
  lazy val connection: String                   = config.getString("connection-name")
  lazy val sseHeartbeatDuration: FiniteDuration = config.getDuration("server-sent-events.keep-alive").toScala
  lazy val httpConnection                       = HttpConnection(ComponentId(connection, ComponentType.Service))
}
