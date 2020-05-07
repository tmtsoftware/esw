package esw.ocs.testkit

import csw.testkit.scaladsl.CSWService

sealed trait Service

object Service {
  def convertToCsw(services: Seq[Service]): Seq[CSWService] = services.collect { case w: WrappedCSWService => w.cswService }

  sealed trait ESWService                                      extends Service
  abstract class WrappedCSWService(val cswService: CSWService) extends Service

  case object LocationServer extends WrappedCSWService(CSWService.LocationServer)
  case object ConfigServer   extends WrappedCSWService(CSWService.ConfigServer)
  case object EventServer    extends WrappedCSWService(CSWService.EventServer)
  case object AlarmServer    extends WrappedCSWService(CSWService.AlarmServer)
  case object Gateway        extends ESWService
  case object MachineAgent   extends ESWService
}
