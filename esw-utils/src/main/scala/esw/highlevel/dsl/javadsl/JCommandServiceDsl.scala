package esw.highlevel.dsl.javadsl

import java.util.concurrent.CompletionStage

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import csw.location.models.ComponentType
import csw.location.models.ComponentType.{Assembly, HCD}
import csw.params.commands.CommandResponse.{OnewayResponse, SubmitResponse}
import csw.params.commands.ControlCommand
import esw.highlevel.dsl.ComponentFactory

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.jdk.FutureConverters.FutureOps

trait JCommandServiceDsl extends ComponentFactory {
  private[esw] def actorSystem: ActorSystem[_]

  implicit val timeout: Timeout = 10.seconds

  def submitCommandToAssembly(assemblyName: String, command: ControlCommand): CompletionStage[SubmitResponse] =
    submit(assemblyName, Assembly, command)

  def submitCommandToHcd(hcdName: String, command: ControlCommand): CompletionStage[SubmitResponse] =
    submit(hcdName, HCD, command)

  def oneWayToAssembly(assemblyName: String, command: ControlCommand): CompletionStage[OnewayResponse] =
    oneWay(assemblyName, Assembly, command)

  def oneWayCommandToHcd(hcdName: String, command: ControlCommand): CompletionStage[OnewayResponse] =
    oneWay(hcdName, HCD, command)

  private def submit(name: String, compType: ComponentType, command: ControlCommand) =
    send(name, compType, _.submit(command))

  private def oneWay(name: String, compType: ComponentType, command: ControlCommand) =
    send(name, compType, _.oneway(command))

  private def send[T](name: String, compType: ComponentType, action: CommandService => Future[T]): CompletionStage[T] =
    commandService(name, compType).flatMap(action)(actorSystem.executionContext).asJava
}
