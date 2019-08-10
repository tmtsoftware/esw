package esw.ocs.api

import esw.ocs.api.models.messages.SimpleResponse

import scala.concurrent.Future

trait SequencerSupervisor {
  def shutdown(): Future[SimpleResponse]
  def abort(): Future[SimpleResponse]
}
