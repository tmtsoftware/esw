package esw.commons.utils.location

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt._
import esw.commons.utils.location.EswLocationError.{
  FindLocationError,
  LocationNotFound,
  RegistrationError,
  RegistrationListingFailed
}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

private[esw] class LocationServiceUtil(val locationService: LocationService)(implicit
    val actorSystem: ActorSystem[_]
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  private def list(componentType: ComponentType): Future[Either[RegistrationListingFailed, List[Location]]] =
    locationService
      .list(componentType)
      .map(Right(_))
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  private def addCoordinatedShutdownTask(coordinatedShutdown: CoordinatedShutdown, registrationResult: RegistrationResult): Unit =
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())

  private[esw] def register(akkaRegistration: AkkaRegistration): Future[Either[RegistrationError, AkkaLocation]] =
    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem), result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .mapError {
        case e: RegistrationFailed        => EswLocationError.RegistrationFailed(e.msg)
        case e: OtherLocationIsRegistered => EswLocationError.OtherLocationIsRegistered(e.msg)
      }

  def listAkkaLocationsBy(
      subsystem: Subsystem,
      componentType: ComponentType
  ): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    listAkkaLocationsBy(componentType, _.prefix.subsystem == subsystem)

  def listAkkaLocationsBy(
      componentName: String,
      componentType: ComponentType
  ): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    listAkkaLocationsBy(componentType, _.prefix.componentName == componentName)

  def listAkkaLocationsBy(
      componentType: ComponentType,
      withFilter: AkkaLocation => Boolean = _ => true
  ): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    list(componentType)
      .mapRight(_.collect {
        case akkaLocation: AkkaLocation if withFilter(akkaLocation) => akkaLocation
      })

  def findByComponentNameAndType(
      componentName: String,
      componentType: ComponentType
  ): Future[Either[FindLocationError, Location]] =
    list(componentType)
      .mapRight(_.find(_.connection.componentId.prefix.componentName == componentName))
      .map {
        case Left(error) => Left(error)
        case Right(maybeLocation) =>
          maybeLocation.toRight(
            LocationNotFound(
              s"Could not find location matching ComponentName: $componentName, componentType: $componentType"
            )
          )
      }

  def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Either[FindLocationError, L]] =
    locationService
      .resolve(connection, within)
      .map {
        case Some(location) => Right(location)
        case None           => Left(LocationNotFound(s"Could not resolve location matching connection: $connection"))
      }
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  def find[L <: Location](connection: TypedConnection[L]): Future[Either[FindLocationError, L]] =
    locationService
      .find(connection)
      .map {
        case Some(location) => Right(location)
        case None           => Left(LocationNotFound(s"Could not find location matching connection: $connection"))
      }
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  private[esw] def resolveSequencer(
      subsystem: Subsystem,
      obsMode: String,
      within: FiniteDuration
  ): Future[Either[FindLocationError, AkkaLocation]] =
    resolve(AkkaConnection(ComponentId(Prefix(subsystem, obsMode), Sequencer)), within)

  private[esw] def findSequencer(
      subsystem: Subsystem,
      obsMode: String
  ): Future[Either[FindLocationError, AkkaLocation]] =
    find(AkkaConnection(ComponentId(Prefix(subsystem, obsMode), Sequencer)))
}
