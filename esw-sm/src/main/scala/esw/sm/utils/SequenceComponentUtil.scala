package esw.sm.utils

import akka.Done
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl

import scala.async.Async._
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout
) {
  import actorSystem.executionContext

  def getAvailableSequenceComponent(subsystem: Subsystem): Future[Either[SequencerError, SequenceComponentApi]] = {
    val maybeSeqCompApiF: Future[Option[SequenceComponentApi]] = subsystem match {
      case ESW => getIdleSequenceComponentFor(ESW)
      case other: Subsystem =>
        val eventualMaybeApi = getIdleSequenceComponentFor(other)
        eventualMaybeApi.flatMap {
          case Some(_) => eventualMaybeApi
          case None    => getIdleSequenceComponentFor(ESW)
        }
    }

    // spawn SeqComp if not able to find available sequence component of subsystem or ESW
    maybeSeqCompApiF.flatMap {
      case Some(value) => Future.successful(Right(value))
      case None        => agentUtil.spawnSequenceComponentFor(subsystem)
    }
  }

  def unloadScript(loc: AkkaLocation): Future[Done] = new SequenceComponentImpl(loc).unloadScript()

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] = {
    locationServiceUtil
      .listBy(subsystem, SequenceComponent)
      .flatMap { locations =>
        // check if these seq comp locations are idle and return first idle sequence component found
        FutureUtils
          .firstCompletedOf(locations.map(idleSequenceComponent))(_.isDefined)
          .map(_.flatten)
      }
  }

  private[sm] def idleSequenceComponent(sequenceComponentLocation: AkkaLocation): Future[Option[SequenceComponentApi]] = async {
    val sequenceComponentApi = new SequenceComponentImpl(sequenceComponentLocation)
    val status               = await(sequenceComponentApi.status)
    status.response.map(_ => sequenceComponentApi)
  }
}
