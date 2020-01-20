package esw.ocs.impl.internal

import java.util.concurrent.CompletionStage

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ComponentType.Sequencer
import csw.location.models.Connection.AkkaConnection
import csw.location.models.ConnectionType.AkkaType
import csw.location.models._
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.protocol.ScriptError

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

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

  def register(akkaRegistration: AkkaRegistration): Future[Either[ScriptError, AkkaLocation]] =
    register(akkaRegistration, onFailure = {
      case NonFatal(e) => Future.successful(Left(ScriptError(e.getMessage)))
    })

  def listBy(subsystem: Subsystem, componentType: ComponentType): Future[List[AkkaLocation]] =
    locationService
      .list(componentType)
      .map(_.collect {
        case akkaLocation: AkkaLocation if akkaLocation.prefix.subsystem == subsystem => akkaLocation
      })

  def resolveByComponentNameAndType(componentName: String, componentType: ComponentType): Future[Option[Location]] =
    locationService.list(componentType).map(_.find(_.connection.componentId.prefix.componentName == componentName))

  def resolveComponentRef(prefix: Prefix, componentType: ComponentType): Future[ActorRef[ComponentMessage]] = {
    val connection = AkkaConnection(ComponentId(prefix, componentType))
    locationService.resolve(connection, Timeouts.DefaultTimeout).map {
      case Some(location: AkkaLocation) => location.componentRef
      case Some(location) =>
        throw new RuntimeException(
          s"Incorrect connection type of the component. Expected $AkkaType, found ${location.connection.connectionType}"
        )
      case None => throw new IllegalArgumentException(s"Could not find any component with name: $prefix")
    }
  }

  private[esw] def resolveSequencer(
      subsystem: Subsystem,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ) =
    locationService
      .resolve(AkkaConnection(ComponentId(Prefix(subsystem, observingMode), Sequencer)), timeout)
      .map {
        case Some(value) => value
        case None        => throw new RuntimeException(s"Could not find any sequencer with name: ${subsystem.name}.$observingMode")
      }

  def resolveAkkaLocation(prefix: Prefix, componentType: ComponentType): Future[AkkaLocation] = {
    val connection = AkkaConnection(ComponentId(prefix, componentType))
    locationService.resolve(connection, Timeouts.DefaultTimeout).map {
      case Some(location: AkkaLocation) => location
      case Some(location) =>
        throw new RuntimeException(
          s"Incorrect connection type of the component. Expected $AkkaType, found ${location.connection.connectionType}"
        )
      case None => throw new IllegalArgumentException(s"Could not find any component with name: $prefix")
    }
  }

  // Added this to be accessed by kotlin
  def jResolveComponentRef(prefix: Prefix, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    resolveComponentRef(prefix, componentType).toJava

  def jResolveAkkaLocation(prefix: Prefix, componentType: ComponentType): CompletionStage[AkkaLocation] =
    resolveAkkaLocation(prefix, componentType).toJava

}
