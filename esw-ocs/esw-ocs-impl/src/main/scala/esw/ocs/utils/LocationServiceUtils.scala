package esw.ocs.utils

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.{AkkaLocation, AkkaRegistration, ComponentType}
import csw.params.core.models.Subsystem
import esw.ocs.api.models.messages.RegistrationError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LocationServiceUtils(locationService: LocationService) {

  private def addCoordinatedShutdownTask(
      coordinatedShutdown: CoordinatedShutdown,
      registrationResult: RegistrationResult
  ): Unit = {
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())
  }

  def register(
      akkaRegistration: AkkaRegistration
  )(implicit actorSystem: ActorSystem[_]): Future[Either[RegistrationError, AkkaLocation]] =
    registerWithRetry(akkaRegistration, 0)

  def registerWithRetry(akkaRegistration: => AkkaRegistration, retryCount: Int)(
      implicit actorSystem: ActorSystem[_]
  ): Future[Either[RegistrationError, AkkaLocation]] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem.toUntyped), result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recoverWith {
        case OtherLocationIsRegistered(_) if retryCount > 0 =>
          registerWithRetry(akkaRegistration, retryCount - 1)
        case NonFatal(e) => Future.successful(Left(RegistrationError(e.getMessage)))
      }
  }

  def listBy(subsystem: Subsystem, componentType: ComponentType)(
      implicit ec: ExecutionContext
  ): Future[List[AkkaLocation]] = {
    locationService
      .list(componentType)
      .map(_.collect {
        case akkaLocation @ AkkaLocation(_, prefix, _) if prefix.subsystem == subsystem => akkaLocation
      })
  }
}
