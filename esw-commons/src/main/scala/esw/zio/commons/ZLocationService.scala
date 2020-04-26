package esw.zio.commons

import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.PhaseBeforeServiceUnbind
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.ComponentMessage
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models._
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.utils.location.EswLocationError
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import zio.{IO, Task, ZIO}

import scala.concurrent.duration.FiniteDuration

class ZLocationService(val locationService: LocationService)(implicit system: ActorSystem[_]) {

  private lazy val coordinatedShutdown = CoordinatedShutdown(system)
  private def addCoordinatedShutdownTask(registrationResult: RegistrationResult): Task[Unit] =
    ZIO.effect(
      coordinatedShutdown.addTask(PhaseBeforeServiceUnbind, s"unregistering-${registrationResult.location}")(() =>
        registrationResult.unregister()
      )
    )

  private def list[F](componentType: ComponentType): IO[RegistrationListingFailed, List[Location]] =
    ZIO
      .fromFuture(_ => locationService.list(componentType))
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  private[esw] def register[E](
      akkaRegistration: AkkaRegistration,
      onFailure: Throwable => IO[E, AkkaLocation]
  ): IO[E, AkkaLocation] =
    ZIO
      .fromFuture(_ => locationService.register(akkaRegistration))
      .tap(addCoordinatedShutdownTask)
      .map(_.location.asInstanceOf[AkkaLocation])
      .catchAll(onFailure)

  def listAkkaLocationsBy(
      subsystem: Subsystem,
      componentType: ComponentType
  ): IO[RegistrationListingFailed, List[AkkaLocation]] =
    list(componentType).map(_.collect {
      case akkaLocation: AkkaLocation if akkaLocation.prefix.subsystem == subsystem => akkaLocation
    })

  def resolveByComponentNameAndType(
      componentName: String,
      componentType: ComponentType
  ): IO[EswLocationError, Location] =
    list(componentType)
      .map(_.find(_.connection.componentId.prefix.componentName == componentName))
      .someOrFail(
        ResolveLocationFailed(s"Could not find location matching ComponentName: $componentName, componentType: $componentType")
      )

  def resolve[L <: Location](
      connection: TypedConnection[L],
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): IO[EswLocationError, L] =
    ZIO
      .fromFuture(_ => locationService.resolve(connection, timeout))
      .someOrFail(ResolveLocationFailed(s"Could not resolve location matching connection: $connection"))
      .mapError(e => RegistrationListingFailed(s"Location Service Error: ${e.getMessage}"))

  def resolveComponentRef(
      prefix: Prefix,
      componentType: ComponentType
  ): IO[EswLocationError, ActorRef[ComponentMessage]] =
    resolve(akkaConnection(prefix, componentType)).map(_.componentRef)

  private[esw] def resolveSequencer(
      subsystem: Subsystem,
      observingMode: String,
      timeout: FiniteDuration = Timeouts.DefaultTimeout
  ): IO[EswLocationError, AkkaLocation] =
    resolve(akkaConnection(Prefix(subsystem, observingMode), Sequencer), timeout)

  private def akkaConnection(prefix: Prefix, compType: ComponentType) = AkkaConnection(ComponentId(prefix, compType))
}
