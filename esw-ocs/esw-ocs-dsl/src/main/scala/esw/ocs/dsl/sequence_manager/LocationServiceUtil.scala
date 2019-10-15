package esw.ocs.dsl.sequence_manager

import java.util.concurrent.CompletionStage

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.pattern.after
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.ConnectionType.AkkaType
import csw.location.models._
import csw.params.core.models.Subsystem
import esw.ocs.api.protocol.LoadScriptError
import esw.ocs.dsl.Timeouts

import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LocationServiceUtil(private[esw] val locationService: LocationService)(implicit val actorSystem: ActorSystem[_]) {
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
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem.toUntyped), result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recoverWith(onFailure)

  def register(akkaRegistration: AkkaRegistration): Future[Either[LoadScriptError, AkkaLocation]] =
    register(akkaRegistration, onFailure = {
      case NonFatal(e) => Future.successful(Left(LoadScriptError(e.getMessage)))
    })

  def listBy(subsystem: Subsystem, componentType: ComponentType): Future[List[AkkaLocation]] =
    locationService
      .list(componentType)
      .map(_.collect {
        case akkaLocation @ AkkaLocation(_, prefix, _) if prefix.subsystem == subsystem => akkaLocation
      })

  //Can be used to listByPackageId() and listByObsMode(), in future. Separate APIs can be created once we have concrete
  //classes for `PackageId` and `ObsMode`
  def listByComponentName(name: String): Future[List[Location]] =
    locationService.list.map(_.filter(_.connection.componentId.name.contains(name)))

  def resolveByComponentNameAndType(name: String, componentType: ComponentType): Future[Option[Location]] =
    locationService.list(componentType).map(_.find(_.connection.componentId.name == name))

  def resolveComponentRef(componentName: String, componentType: ComponentType): Future[ActorRef[ComponentMessage]] = {
    val connection = AkkaConnection(ComponentId(componentName, componentType))
    locationService.resolve(connection, Timeouts.DefaultTimeout).map {
      case Some(location: AkkaLocation) => location.componentRef
      case Some(location) =>
        throw new RuntimeException(
          s"Incorrect connection type of the component. Expected $AkkaType, found ${location.connection.connectionType}"
        )
      case None => throw new IllegalArgumentException(s"Could not find any component with name: $componentName")
    }
  }

  private[esw] def resolveSequencer(
      packageId: String,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): Future[AkkaLocation] = {
    val ResolveInterval = 50.millis
    def resolveLoop(remainingDuration: FiniteDuration): Future[AkkaLocation] =
      locationService.list
        .map {
          _.collectFirst {
            case location: AkkaLocation if location.connection.componentId.name.contains(s"$packageId@$observingMode") =>
              location
          }
        }
        .flatMap {
          case Some(location) => Future.successful(location)
          case _ if remainingDuration.length <= 0 =>
            throw new RuntimeException(s"Could not find any sequencer with name: $packageId@$observingMode")
          case _ =>
            after(remainingDuration min ResolveInterval, actorSystem.scheduler) {
              resolveLoop(remainingDuration minus ResolveInterval)
            }
        }

    resolveLoop(timeout)
  }

  // Added this to be accessed by kotlin
  def jResolveComponentRef(componentName: String, componentType: ComponentType): CompletionStage[ActorRef[ComponentMessage]] =
    resolveComponentRef(componentName, componentType).toJava

}
