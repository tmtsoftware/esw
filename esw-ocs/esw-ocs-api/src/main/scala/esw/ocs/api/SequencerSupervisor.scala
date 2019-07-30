package esw.ocs.api

import esw.ocs.api.models.messages.LifecycleResponse

import scala.concurrent.Future

trait SequencerSupervisor {

  def shutdown(): Future[LifecycleResponse]
  def abort(): Future[LifecycleResponse]

}
