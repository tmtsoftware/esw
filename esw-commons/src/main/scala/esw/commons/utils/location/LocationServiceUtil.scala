package esw.commons.utils.location

import akka.actor.CoordinatedShutdown
import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt._
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

private[esw] class LocationServiceUtil(val locationService: LocationService)(
    implicit val actorSystem: ActorSystem[_]
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

  private def list(componentType: ComponentType): Future[Either[RegistrationListingFailed, List[Location]]] =
    locationService
      .list(componentType)
      .map(Right(_))
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  private def addCoordinatedShutdownTask(
      coordinatedShutdown: CoordinatedShutdown,
      registrationResult: RegistrationResult
  ): Unit =
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())

  private[esw] def register[E](
      akkaRegistration: AkkaRegistration,
      onFailure: PartialFunction[Throwable, Future[Either[E, AkkaLocation]]]
  ): Future[Either[E, AkkaLocation]] =
    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem), result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recoverWith(onFailure)

  def listAkkaLocationsBy(
      subsystem: Subsystem,
      componentType: ComponentType
  ): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    list(componentType)
      .mapRight(_.collect {
        case akkaLocation: AkkaLocation if akkaLocation.prefix.subsystem == subsystem => akkaLocation
      })

  def resolveByComponentNameAndType(
      componentName: String,
      componentType: ComponentType
  ): Future[Either[EswLocationError, Location]] =
    list(componentType)
      .mapRight(_.find(_.connection.componentId.prefix.componentName == componentName))
      .map {
        case Left(error) => Left(error)
        case Right(maybeLocation) =>
          maybeLocation.toRight(
            ResolveLocationFailed(
              s"Could not find location matching ComponentName: $componentName, componentType: $componentType"
            )
          )
      }

  def resolve[L <: Location](
      connection: TypedConnection[L],
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): Future[Either[EswLocationError, L]] =
    locationService
      .resolve(connection, timeout)
      .map {
        case Some(location) => Right(location)
        case None           => Left(ResolveLocationFailed(s"Could not resolve location matching connection: $connection"))
      }
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  private[esw] def resolveSequencer(
      subsystem: Subsystem,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): Future[Either[EswLocationError, AkkaLocation]] =
    resolve(AkkaConnection(ComponentId(Prefix(subsystem, observingMode), Sequencer)), timeout)
}
