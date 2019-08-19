package esw.ocs.api

import esw.ocs.api.models.messages.OkOrUnhandledResponse

import scala.concurrent.Future

trait SequencerSupervisor {
  def shutdown(): Future[OkOrUnhandledResponse]
  def abortSequence(): Future[OkOrUnhandledResponse]
}
