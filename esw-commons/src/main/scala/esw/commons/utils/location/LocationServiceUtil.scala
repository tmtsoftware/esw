package esw.commons.utils.location

import java.util.concurrent.CompletionStage

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt._
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory

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
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem.toClassic), result)
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

  def resolveComponentRef(
      prefix: Prefix,
      componentType: ComponentType
  ): Future[Either[EswLocationError, ActorRef[ComponentMessage]]] =
    resolve(AkkaConnection(ComponentId(prefix, componentType))).mapRight(_.componentRef)

  private[esw] def resolveSequencer(
      subsystem: Subsystem,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): Future[Either[EswLocationError, SequencerApi]] =
    resolve(AkkaConnection(ComponentId(Prefix(subsystem, observingMode), Sequencer)), timeout)
      .mapRight(SequencerApiFactory.make)

  // Added this to be accessed by kotlin
  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    resolveComponentRef(prefix, componentType).toJava

  def jResolveAkkaLocation(prefix: Prefix, componentType: ComponentType): CompletionStage[AkkaLocation] =
    resolve(AkkaConnection(ComponentId(prefix, componentType))).toJava

}
