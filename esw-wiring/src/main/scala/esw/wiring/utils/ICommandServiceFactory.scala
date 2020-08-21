package esw.wiring.utils

import akka.actor.typed.ActorSystem
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.location.api.models.Location

trait ICommandServiceFactory {
  def make(componentLocation: Location)(implicit actorSystem: ActorSystem[_]): CommandService
}

object ICommandServiceFactory {
  def default: ICommandServiceFactory =
    new ICommandServiceFactory {
      override def make(componentLocation: Location)(implicit actorSystem: ActorSystem[_]): CommandService =
        CommandServiceFactory.make(componentLocation)
    }
}
