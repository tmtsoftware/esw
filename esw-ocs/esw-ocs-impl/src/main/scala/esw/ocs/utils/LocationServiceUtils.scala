package esw.ocs.utils

import akka.actor.CoordinatedShutdown
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

  def register(akkaRegistration: AkkaRegistration)(
      coordinatedShutdown: CoordinatedShutdown
  )(implicit ec: ExecutionContext): Future[Either[RegistrationError, AkkaLocation]] =
    registerWithRetry(akkaRegistration, 0)(coordinatedShutdown)

  def registerWithRetry(akkaRegistration: AkkaRegistration, retryCount: Int)(
      coordinatedShutdown: CoordinatedShutdown
  )(implicit ec: ExecutionContext): Future[Either[RegistrationError, AkkaLocation]] = {
    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(coordinatedShutdown, result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recoverWith {
        case OtherLocationIsRegistered(_) if retryCount > 0 =>
          registerWithRetry(akkaRegistration, retryCount - 1)(coordinatedShutdown)
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
