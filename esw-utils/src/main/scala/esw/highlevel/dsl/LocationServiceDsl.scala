package esw.highlevel.dsl

import akka.actor.CoordinatedShutdown
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.models.ConnectionType.AkkaType
import csw.location.models._
import csw.params.core.models.Subsystem
import esw.ocs.api.protocol.RegistrationError

import scala.async.Async._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class LocationServiceUtil(val locationService: LocationService)(implicit val actorSystem: ActorSystem[_])
    extends LocationServiceDsl

trait LocationServiceDsl {

  implicit protected val actorSystem: ActorSystem[_]

  private[esw] def locationService: LocationService
  private def addCoordinatedShutdownTask(
      coordinatedShutdown: CoordinatedShutdown,
      registrationResult: RegistrationResult
  ): Unit = {
    coordinatedShutdown.addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      s"unregistering-${registrationResult.location}"
    )(() => registrationResult.unregister())
  }

  private[esw] def register[E](
      akkaRegistration: AkkaRegistration,
      onFailure: PartialFunction[Throwable, Future[Either[E, AkkaLocation]]]
  )(implicit actorSystem: ActorSystem[_]): Future[Either[E, AkkaLocation]] = {
    implicit val ec: ExecutionContext = actorSystem.executionContext
    locationService
      .register(akkaRegistration)
      .map { result =>
        addCoordinatedShutdownTask(CoordinatedShutdown(actorSystem.toUntyped), result)
        Right(result.location.asInstanceOf[AkkaLocation])
      }
      .recoverWith(onFailure)
  }

  def register(
      akkaRegistration: AkkaRegistration
  )(implicit actorSystem: ActorSystem[_]): Future[Either[RegistrationError, AkkaLocation]] =
    register(akkaRegistration, onFailure = {
      case NonFatal(e) => Future.successful(Left(RegistrationError(e.getMessage)))
    })

  def listBy(subsystem: Subsystem, componentType: ComponentType)(implicit ec: ExecutionContext): Future[List[AkkaLocation]] = {
    locationService
      .list(componentType)
      .map(_.collect {
        case akkaLocation @ AkkaLocation(_, prefix, _) if prefix.subsystem == subsystem => akkaLocation
      })
  }

  //Can be used to listBySequencerId() and listByObsMode(), in future. Separate APIs can be created once we have concrete
  //classes for `SequencerId` and `ObsMode`
  def listByComponentName(name: String)(implicit ec: ExecutionContext): Future[List[Location]] = {
    locationService.list.map { locations =>
      locations.filter(x => x.connection.componentId.name.contains(name))
    }
  }

  def resolveByComponentNameAndType(name: String, componentType: ComponentType)(
      implicit ec: ExecutionContext
  ): Future[Option[Location]] = async {
    await(locationService.list(componentType))
      .find(location => location.connection.componentId.name == name)
  }

  def resolveComponentRef(componentName: String, componentType: ComponentType)(
      implicit ec: ExecutionContext
  ): Future[ActorRef[ComponentMessage]] = {
    resolveByComponentNameAndType(componentName, componentType).map {
      case Some(location: AkkaLocation) => location.componentRef
      case Some(location) =>
        throw new RuntimeException(
          s"Incorrect connection type of the component. Expected $AkkaType, found ${location.connection.connectionType}"
        )
      case None => throw new IllegalArgumentException(s"Could not find any component with name: $componentName")
    }
  }

  // To be used by Script Writer
  def resolveSequencer(sequencerId: String, observingMode: String)(
      implicit ec: ExecutionContext
  ): Future[AkkaLocation] =
    async {
      await(locationService.list)
        .find(location => location.connection.componentId.name.contains(s"$sequencerId@$observingMode"))
    }.collect {
      case Some(location: AkkaLocation) => location
      case Some(location) =>
        throw new RuntimeException(s"Sequencer is registered with wrong connection type: ${location.connection.connectionType}")
      case None => throw new IllegalArgumentException(s"Could not find any sequencer with name: $sequencerId@$observingMode")
    }
}
