package esw.ocs.api

import esw.ocs.api.models.messages.SequencerResponses.LifecycleResponse

import scala.concurrent.Future

trait SequencerSupervisor {

  def shutdown(): Future[LifecycleResponse]
  def abort(): Future[LifecycleResponse]

}
