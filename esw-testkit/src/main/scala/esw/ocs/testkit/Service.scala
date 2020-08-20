package esw.ocs.testkit

import csw.testkit.scaladsl.CSWService
import scala.language.implicitConversions

sealed trait Service

object Service {
  def convertToCsw(services: Seq[Service]): Seq[CSWService] =
    services.collect { case WrappedCSWService(cswService) => cswService }

  implicit def toEswService(cswService: CSWService): WrappedCSWService = WrappedCSWService(cswService)

  case class WrappedCSWService(cswService: CSWService) extends Service
  case object Gateway                                  extends Service
  case object MachineAgent                             extends Service
  case object AAS                                      extends Service
  case object SequenceManager                          extends Service
}
