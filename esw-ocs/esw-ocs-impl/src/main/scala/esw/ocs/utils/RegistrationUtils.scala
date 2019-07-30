package esw.ocs.utils

import akka.actor.CoordinatedShutdown
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.{AkkaLocation, AkkaRegistration}
import esw.ocs.api.models.messages.error.RegistrationError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

object RegistrationUtils {

  def register(locationService: LocationService, akkaRegistration: AkkaRegistration)(
      coordinatedShutdown: CoordinatedShutdown
  )(implicit ec: ExecutionContext): Future[Either[RegistrationError, AkkaLocation]] = {

    def addCoordinatedShutdownTask(registrationResult: RegistrationResult): Unit = {
      coordinatedShutdown.addTask(
        CoordinatedShutdown.PhaseBeforeServiceUnbind,
        s"unregistering-${registrationResult.location}"
      )(() => registrationResult.unregister())
    }

    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recover {
        case NonFatal(e) => Left(RegistrationError(e.getMessage))
      }
  }
}
