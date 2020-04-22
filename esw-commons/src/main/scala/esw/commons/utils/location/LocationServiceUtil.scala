package esw.commons.utils.location

import java.util.concurrent.CompletionStage

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.exceptions.{RegistrationListingFailed => CswRegistrationListingFailed}
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.utils.FutureEitherUtils._
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}

private[esw] class LocationServiceUtil(val locationService: LocationService)(
    implicit val actorSystem: ActorSystem[_]
) {
  implicit val ec: ExecutionContext = actorSystem.executionContext

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

  def listBy(subsystem: Subsystem, componentType: ComponentType): Future[Either[RegistrationListingFailed, List[AkkaLocation]]] =
    locationService
      .list(componentType)
      .map(_.collect {
        case akkaLocation: AkkaLocation if akkaLocation.prefix.subsystem == subsystem => akkaLocation
      })
      .map(Right(_))
      .recover {
        case _: CswRegistrationListingFailed => Left(RegistrationListingFailed(s"$componentType"))
      }

  def resolveByComponentNameAndType(
      componentName: String,
      componentType: ComponentType
  ): Future[Either[EswLocationError, Location]] =
    locationService
      .list(componentType)
      .map(_.find(_.connection.componentId.prefix.componentName == componentName))
      .map {
        case Some(location) => Right(location)
        case None =>
          Left(
            ResolveLocationFailed(
              s"Could not find location matching ComponentName: $componentName, componentType: $componentType")
          )
      }
      .recover {
        case _: CswRegistrationListingFailed => Left(RegistrationListingFailed(s"$componentName and $componentType"))
      }

  def resolveComponentRef(
      prefix: Prefix,
      componentType: ComponentType
  ): Future[Either[EswLocationError, ActorRef[ComponentMessage]]] =
    resolveAkkaLocation(prefix, componentType).right(_.componentRef)

  private[esw] def resolveSequencer(
      subsystem: Subsystem,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): Future[Either[EswLocationError, AkkaLocation]] = {
    val componentId = ComponentId(Prefix(subsystem, observingMode), Sequencer)
    locationService
      .resolve(AkkaConnection(componentId), timeout)
      .map {
        case Some(location) => Right(location)
        case None =>
          Left(
            ResolveLocationFailed(
              s"Could not find location matching subsystem: $subsystem, ObservingMode: $observingMode, ComponentType: $Sequencer"
            )
          )
      }
      .recover {
        case _: CswRegistrationListingFailed => Left(RegistrationListingFailed(s"$componentId "))
      }
  }

  def resolveAkkaLocation(prefix: Prefix, componentType: ComponentType): Future[Either[EswLocationError, AkkaLocation]] = {
    val connection = AkkaConnection(ComponentId(prefix, componentType))
    locationService
      .resolve(connection, Timeouts.DefaultTimeout)
      .map {
        case Some(location) => Right(location)
        case None           => Left(ResolveLocationFailed(s"Could not find location matching $prefix"))
      }
      .recover {
        case _: CswRegistrationListingFailed => Left(RegistrationListingFailed(s"$connection"))
      }
  }

  // todo: error should extend exception and same should be carries when converted to java
  // Added this to be accessed by kotlin
  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] = {
    resolveComponentRef(prefix, componentType).map {
      case Left(error)     => throw new RuntimeException(s"Resolve Akka Location failed with ${error.msg}")
      case Right(actorRef) => actorRef
    }.toJava
  }

  def jResolveAkkaLocation(prefix: Prefix, componentType: ComponentType): CompletionStage[AkkaLocation] =
    resolveAkkaLocation(prefix, componentType).map {
      case Left(error)         => throw new RuntimeException(s"Resolve Akka Location failed with ${error.msg}")
      case Right(akkaLocation) => akkaLocation
    }.toJava

}
