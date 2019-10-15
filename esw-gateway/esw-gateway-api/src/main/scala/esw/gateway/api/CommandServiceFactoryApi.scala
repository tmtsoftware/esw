package esw.gateway.api

import csw.command.api.scaladsl.CommandService
import csw.location.models.ComponentId
import esw.gateway.api.protocol.InvalidComponent

import scala.concurrent.Future

trait CommandServiceFactoryApi {
  def commandService(componentId: ComponentId): Future[Either[InvalidComponent, CommandService]]
}
