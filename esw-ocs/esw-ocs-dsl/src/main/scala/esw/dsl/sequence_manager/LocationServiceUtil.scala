package esw.dsl.sequence_manager

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.ConnectionType.AkkaType
import csw.location.models._
import csw.params.core.models.Subsystem
import esw.dsl.Timeouts
import esw.dsl.script.services.LocationServiceDsl
import esw.ocs.api.protocol.RegistrationError

import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LocationServiceUtil(private[esw] val locationService: LocationService)(implicit val actorSystem: ActorSystem[_])
    extends LocationServiceDsl {
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

  def register(akkaRegistration: AkkaRegistration): Future[Either[RegistrationError, AkkaLocation]] =
    register(akkaRegistration, onFailure = {
      case NonFatal(e) => Future.successful(Left(RegistrationError(e.getMessage)))
    })

  def listBy(subsystem: Subsystem, componentType: ComponentType): Future[List[AkkaLocation]] =
    locationService
      .list(componentType)
      .map(_.collect {
        case akkaLocation @ AkkaLocation(_, prefix, _) if prefix.subsystem == subsystem => akkaLocation
      })

  //Can be used to listBySequencerId() and listByObsMode(), in future. Separate APIs can be created once we have concrete
  //classes for `SequencerId` and `ObsMode`
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
}
