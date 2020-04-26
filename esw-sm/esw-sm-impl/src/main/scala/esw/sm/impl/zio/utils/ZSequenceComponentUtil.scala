package esw.sm.impl.zio.utils

import akka.Done
import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.SequenceComponent
import csw.prefix.models.Subsystem
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.client.SequenceComponentImpl
import esw.sm.api.models.AgentError
import esw.zio.commons.{ZIOUtils, ZLocationService}
import zio.{IO, Task, ZIO}

class ZSequenceComponentUtil(ZLocationService: ZLocationService, ZAgent: ZAgentUtil)(
    implicit actorSystem: ActorSystem[_],
    timeout: Timeout
) {
  def getAvailableSequenceComponent(subsystem: Subsystem): IO[AgentError, SequenceComponentApi] =
    getIdleSequenceComponentFor(subsystem)
      .catchAll { _ =>
        val fallback = if (subsystem != ESW) getIdleSequenceComponentFor(ESW) else ZIO.fail(())
        fallback.catchAll(_ => ZAgent.spawnSequenceComponentFor(subsystem))
      }

  def unloadScript(loc: AkkaLocation): Task[Done] = ZIO.fromFuture(_ => new SequenceComponentImpl(loc).unloadScript())

  private def getIdleSequenceComponentFor(subsystem: Subsystem): IO[Unit, SequenceComponentApi] =
    ZLocationService
      .listAkkaLocationsBy(subsystem, SequenceComponent)
      .flatMap(raceForIdleSequenceComponents)
      .some
      .orElseFail(())

  private def raceForIdleSequenceComponents(locations: List[AkkaLocation]) =
    ZIOUtils
      .firstCompletedOf(locations.map(idleSequenceComponent))(_.isDefined)
      .map(_.flatten)

  private[sm] def idleSequenceComponent(sequenceComponentLocation: AkkaLocation): Task[Option[SequenceComponentApi]] = {
    val api = new SequenceComponentImpl(sequenceComponentLocation)
    ZIO.fromFuture(_ => api.status).map(_.response.map(_ => api))
  }
}
