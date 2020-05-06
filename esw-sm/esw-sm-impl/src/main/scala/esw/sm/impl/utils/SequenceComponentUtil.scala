package esw.sm.impl.utils

import akka.Done
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.models.AgentError

import scala.async.Async._
import scala.concurrent.Future

class SequenceComponentUtil(locationServiceUtil: LocationServiceUtil, agentUtil: AgentUtil)(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout
) {
  import actorSystem.executionContext

  def getAvailableSequenceComponent(subsystem: Subsystem): Future[Either[AgentError, SequenceComponentApi]] =
    getIdleSequenceComponentFor(subsystem)
      .flatMap {
        case api @ Some(_)            => Future.successful(api)
        case None if subsystem != ESW => getIdleSequenceComponentFor(ESW) // fallback
        case None                     => Future.successful(None)
      }
      .flatMap {
        case Some(value) => Future.successful(Right(value))
        // spawn SeqComp if not able to find available sequence component of subsystem or ESW
        case None => agentUtil.spawnSequenceComponentFor(subsystem)
      }

  def unloadScript(loc: AkkaLocation): Future[Done] = new SequenceComponentImpl(loc).unloadScript()

  private def getIdleSequenceComponentFor(subsystem: Subsystem): Future[Option[SequenceComponentApi]] =
    locationServiceUtil
      .listAkkaLocationsBy(subsystem, SequenceComponent)
      .flatMapToAdt(raceForIdleSequenceComponents, _ => None)
  // intentionally ignoring Left as in this case domain won't decide action based on what is error hence converting it to optionality

  private def raceForIdleSequenceComponents(locations: List[AkkaLocation]) =
    FutureUtils
      .firstCompletedOf(locations.map(idleSequenceComponent))(_.isDefined)
      .map(_.flatten)

  private[sm] def idleSequenceComponent(sequenceComponentLocation: AkkaLocation): Future[Option[SequenceComponentApi]] = async {
    val sequenceComponentApi   = new SequenceComponentImpl(sequenceComponentLocation)
    val status                 = await(sequenceComponentApi.status)
    val isBusyRunningSequencer = status.response.isDefined
    if (isBusyRunningSequencer) None else Some(sequenceComponentApi)
  }
}
