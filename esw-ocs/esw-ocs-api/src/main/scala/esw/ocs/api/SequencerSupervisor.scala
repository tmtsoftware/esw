package esw.ocs.api

import esw.ocs.api.models.messages.{AbortResponse, ShutdownResponse}

import scala.concurrent.Future

trait SequencerSupervisor {
  def shutdown(): Future[ShutdownResponse]
  def abort(): Future[AbortResponse]
}
