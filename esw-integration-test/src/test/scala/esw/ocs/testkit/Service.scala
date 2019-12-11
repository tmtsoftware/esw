package esw.ocs.testkit

import csw.testkit.scaladsl.CSWService
import esw.ocs.testkit.Service._

sealed trait Service {
  private def toCswService: Option[CSWService] = this match {
    case LocationServer => Some(CSWService.LocationServer)
    case ConfigServer   => Some(CSWService.ConfigServer)
    case EventServer    => Some(CSWService.EventServer)
    case AlarmServer    => Some(CSWService.AlarmServer)
    case Gateway        => None
  }
}
object Service {
  def convertToCsw(services: Seq[Service]): Seq[CSWService] = services.flatMap(_.toCswService)

  case object LocationServer extends Service
  case object ConfigServer   extends Service
  case object EventServer    extends Service
  case object AlarmServer    extends Service
  case object Gateway        extends Service
}
