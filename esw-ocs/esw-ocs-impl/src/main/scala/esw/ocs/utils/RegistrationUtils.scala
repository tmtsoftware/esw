package esw.ocs.utils

import akka.actor.CoordinatedShutdown
import csw.location.api.exceptions.OtherLocationIsRegistered
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.{AkkaLocation, AkkaRegistration}
import esw.ocs.api.models.messages.RegistrationError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object RegistrationUtils {

  private def addCoordinatedShutdownTask(
      coordinatedShutdown: CoordinatedShutdown,
      registrationResult: RegistrationResult
  ): Unit = {
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())
  }

  def register(locationService: LocationService, akkaRegistration: AkkaRegistration)(
      coordinatedShutdown: CoordinatedShutdown
  )(implicit ec: ExecutionContext): Future[Either[RegistrationError, AkkaLocation]] =
    registerWithRetry(locationService, akkaRegistration, 0)(coordinatedShutdown)

  def registerWithRetry(locationService: LocationService, akkaRegistration: AkkaRegistration, retryCount: Int)(
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
          registerWithRetry(locationService, akkaRegistration, retryCount - 1)(coordinatedShutdown)
        case NonFatal(e) => Future.successful(Left(RegistrationError(e.getMessage)))
      }
  }

}
