package esw.ocs.core

import akka.util.Timeout
import csw.command.client.CommandResponseManager
import esw.ocs.api.models.Step
import esw.ocs.dsl.Async.async
import esw.ocs.macros.StrandEc

import scala.concurrent.Future

private[ocs] class Sequencer(crm: CommandResponseManager)(implicit strandEc: StrandEc, timeout: Timeout) {

  def mayBeNext: Future[Option[Step]] = async(stepList.nextExecutable)

}
