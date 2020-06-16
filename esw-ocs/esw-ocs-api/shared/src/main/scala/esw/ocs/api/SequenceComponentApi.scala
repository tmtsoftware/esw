package esw.ocs.api

import csw.prefix.models.Subsystem
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponseOrUnhandled, OkOrUnhandled, ScriptResponseOrUnhandled}

import scala.concurrent.Future

trait SequenceComponentApi {
  def loadScript(subsystem: Subsystem, observingMode: String): Future[ScriptResponseOrUnhandled]
  def restart(): Future[ScriptResponseOrUnhandled]
  def unloadScript(): Future[OkOrUnhandled]
  def status: Future[GetStatusResponseOrUnhandled]
  def shutdown(): Future[OkOrUnhandled]
}
