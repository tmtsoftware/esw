package esw.commons.utils.location

import org.apache.pekko.actor.CoordinatedShutdown
import org.apache.pekko.actor.typed.ActorSystem
import csw.location.api.exceptions.{OtherLocationIsRegistered, RegistrationFailed}
import csw.location.api.models.*
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.*
import esw.commons.utils.location.EswLocationError.*

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

/**
 * A common utility class on top of location service.
 * @param locationService - an instance of locationService
 * @param actorSystem - an implicit actor system
 */
class LocationServiceUtil(val locationService: LocationService)(implicit
    val actorSystem: ActorSystem[?]
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

  private[esw] def register(pekkoRegistration: PekkoRegistration): Future[Either[RegistrationError, PekkoLocation]] =
    locationService
      .register(pekkoRegistration)
      .map { result =>
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem), result)
        Right(result.location.asInstanceOf[PekkoLocation])
      }
      .mapError {
        case e: RegistrationFailed        => EswLocationError.RegistrationFailed(e.msg)
        case e: OtherLocationIsRegistered => EswLocationError.OtherLocationIsRegistered(e.msg)
        case x                            => throw new MatchError(x)
      }
  def list(componentId: ComponentId): Future[List[Location]] = {
    locationService.list.map(_.filter(loc => loc.connection.componentId == componentId))
  }

  def listPekkoLocationsBy(
      subsystem: Subsystem,
      componentType: ComponentType
  ): Future[Either[RegistrationListingFailed, List[PekkoLocation]]] =
    listPekkoLocationsBy(componentType, _.prefix.subsystem == subsystem)

  def listPekkoLocationsBy(
      componentName: String,
      componentType: ComponentType
  ): Future[Either[RegistrationListingFailed, List[PekkoLocation]]] =
    listPekkoLocationsBy(componentType, _.prefix.componentName == componentName)

  def listSequencersPekkoLocationsBy(
      obsMode: String
  ): Future[Either[RegistrationListingFailed, List[PekkoLocation]]] =
    listPekkoLocationsBy(Sequencer, getObsModeString(_) == obsMode)

  def listPekkoLocationsBy(
      componentType: ComponentType,
      withFilter: PekkoLocation => Boolean = _ => true
  ): Future[Either[RegistrationListingFailed, List[PekkoLocation]]] =
    list(componentType)
      .mapRight(_.collect {
        case pekkoLocation: PekkoLocation if withFilter(pekkoLocation) => pekkoLocation
      })

  def find[L <: Location](connection: TypedConnection[L]): Future[Either[FindLocationError, L]] =
    locationService
      .find(connection)
      .map {
        case Some(location) => Right(location)
        case None           => Left(LocationNotFound(s"Could not find location matching connection: $connection"))
      }
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

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

  def findAgentByHostname(hostName: String): Future[Either[FindLocationError, PekkoLocation]] =
    locationService
      .list(hostName)
      .map { locations =>
        locations
          .find(_.connection.componentId.componentType == ComponentType.Machine)
          .collect { case a: PekkoLocation => a }
          .toRight(LocationNotFound(s"No agent running on host: $hostName"))
      }
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  def findPekkoLocation(prefix: String, componentType: ComponentType): Future[Either[FindLocationError, PekkoLocation]] =
    find(PekkoConnection(ComponentId(Prefix(prefix), componentType)))

  def resolve[L <: Location](connection: TypedConnection[L], within: FiniteDuration): Future[Either[FindLocationError, L]] =
    locationService
      .resolve(connection, within)
      .map {
        case Some(location) => Right(location)
        case None           => Left(LocationNotFound(s"Could not resolve location matching connection: $connection"))
      }
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  private[esw] def resolveSequencer(prefix: Prefix, within: FiniteDuration) =
    resolve(PekkoConnection(ComponentId(prefix, Sequencer)), within)

  private[esw] def findSequencer(prefix: Prefix) =
    find(PekkoConnection(ComponentId(prefix, Sequencer)))

  private def getObsModeString(location: PekkoLocation): String = {
    location.prefix.componentName.split('.').toList match {
      case Nil => throw new RuntimeException("empty component name") // Not Applicable. Prefix always has non-empty component name
      case obsMode :: _ => obsMode
    }
  }
}
