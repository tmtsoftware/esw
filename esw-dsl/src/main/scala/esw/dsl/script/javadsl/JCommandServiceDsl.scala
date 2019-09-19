package esw.dsl.script.javadsl

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.scaladsl.LocationService
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.params.commands.CommandResponse.{OnewayResponse, SubmitResponse, ValidateResponse}
import csw.params.commands.ControlCommand
import esw.dsl.Timeouts

import scala.concurrent.Future
import scala.jdk.FutureConverters.FutureOps

trait JCommandServiceDsl {
  private[esw] val _locationService: LocationService
  private[esw] def actorSystem: ActorSystem[_]

  implicit val timeout: Timeout = Timeouts.DefaultTimeout

  // ====== Assembly =========
  def validateAssemblyCommand(assemblyName: String, command: ControlCommand): CompletionStage[ValidateResponse] =
    validate(assemblyName, Assembly, command)

  def submitCommandToAssembly(assemblyName: String, command: ControlCommand): CompletionStage[SubmitResponse] =
    submit(assemblyName, Assembly, command)

  def submitAndWaitCommandToAssembly(assemblyName: String, command: ControlCommand): CompletionStage[SubmitResponse] =
    submitAndWait(assemblyName, Assembly, command)

  def oneWayCommandToAssembly(assemblyName: String, command: ControlCommand): CompletionStage[OnewayResponse] =
    oneWay(assemblyName, Assembly, command)

  // ====== HCD =========
  def validateHcdCommand(hcdName: String, command: ControlCommand): CompletionStage[ValidateResponse] =
    validate(hcdName, HCD, command)

  def submitCommandToHcd(hcdName: String, command: ControlCommand): CompletionStage[SubmitResponse] =
    submit(hcdName, HCD, command)

  def submitAndWaitCommandToHcd(hcdName: String, command: ControlCommand): CompletionStage[SubmitResponse] =
    submitAndWait(hcdName, HCD, command)

  def oneWayCommandToHcd(hcdName: String, command: ControlCommand): CompletionStage[OnewayResponse] =
    oneWay(hcdName, HCD, command)

  // ============ INTERNAL =============
  private def submit(name: String, compType: ComponentType, command: ControlCommand) =
    send(name, compType, _.submit(command))

  private def submitAndWait(name: String, compType: ComponentType, command: ControlCommand) =
    send(name, compType, _.submitAndWait(command))

  private def oneWay(name: String, compType: ComponentType, command: ControlCommand) =
    send(name, compType, _.oneway(command))

  private def validate(name: String, compType: ComponentType, command: ControlCommand) =
    send(name, compType, _.validate(command))

  private def send[T](name: String, compType: ComponentType, action: CommandService => Future[T]): CompletionStage[T] =
    resolve(name, compType)(CommandServiceFactory.make(_)(actorSystem)).flatMap(action)(actorSystem.executionContext).asJava

  private[dsl] def resolve[T](componentName: String, componentType: ComponentType)(f: AkkaLocation => T): Future[T] =
    _locationService
      .resolve(AkkaConnection(ComponentId(componentName, componentType)), Timeouts.DefaultTimeout)
      .map {
        case Some(akkaLocation) => f(akkaLocation)
        case None               => throw new IllegalArgumentException(s"Could not find component - $componentName of type - $componentType")
      }(actorSystem.executionContext)
}
