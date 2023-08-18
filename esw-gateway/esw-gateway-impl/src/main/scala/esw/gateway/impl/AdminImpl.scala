package esw.gateway.impl

import org.apache.pekko.Done
import org.apache.pekko.actor.typed.ActorSystem
import org.apache.pekko.actor.typed.scaladsl.AskPattern.{Askable, _}
import org.apache.pekko.util.Timeout
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.command.client.messages.ComponentCommonMessage.GetSupervisorLifecycleState
import csw.command.client.messages.ContainerCommonMessage.GetContainerLifecycleState
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.{ComponentMessage, ContainerMessage, GetComponentLogMetadata, SetComponentLogLevel}
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.command.client.models.framework.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.location.api.models.{PekkoLocation, ComponentId, ComponentType}
import csw.location.api.models.ComponentType.*
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.Prefix
import esw.constants.AdminTimeouts
import esw.gateway.api.AdminApi
import esw.gateway.api.protocol.InvalidComponent

import scala.concurrent.Future

/**
 * Pekko actor client for the Admin service
 *
 * @param locationService - an instance of locationService
 * @param actorSystem - an implicit Pekko ActorSystem
 */
class AdminImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AdminApi {
  import actorSystem.executionContext
  private val log: Logger       = AdminLogger.getLogger
  implicit val timeout: Timeout = AdminTimeouts.GetLogMetadata

  private def findComponent(componentId: ComponentId): Future[PekkoLocation] =
    locationService
      .find(PekkoConnection(componentId))
      .map(_.getOrElse(throw InvalidComponent(s"Could not find component : $componentId")))

  private def sendMessageToComponent[T <: ComponentMessage with ContainerMessage](componentId: ComponentId, message: T) =
    findComponent(componentId).map(pekkoLocation => {
      componentId.componentType match {
        case Container => pekkoLocation.containerRef ! message
        case _         => pekkoLocation.componentRef ! message
      }
      Done
    })

  override def shutdown(componentId: ComponentId): Future[Done]  = sendMessageToComponent(componentId, Shutdown)
  override def restart(componentId: ComponentId): Future[Done]   = sendMessageToComponent(componentId, Restart)
  override def goOffline(componentId: ComponentId): Future[Done] = sendMessageToComponent(componentId, Lifecycle(GoOffline))
  override def goOnline(componentId: ComponentId): Future[Done]  = sendMessageToComponent(componentId, Lifecycle(GoOnline))

  override def getComponentLifecycleState(componentId: ComponentId): Future[SupervisorLifecycleState] =
    findComponent(componentId).flatMap(pekkoLocation => pekkoLocation.componentRef ? GetSupervisorLifecycleState.apply)

  override def getContainerLifecycleState(prefix: Prefix): Future[ContainerLifecycleState] =
    findComponent(ComponentId(prefix, ComponentType.Container)).flatMap(pekkoLocation =>
      pekkoLocation.containerRef ? GetContainerLifecycleState.apply
    )

  override def getLogMetadata(componentId: ComponentId): Future[LogMetadata] = {
    val prefix = componentId.prefix

    findComponent(componentId)
      .flatMap(pekkoLocation => {
        log.info(
          "Getting log information from logging system",
          Map("prefix" -> prefix.toString, "location" -> pekkoLocation.toString)
        )
        componentId.componentType match {
          case Sequencer => pekkoLocation.sequencerRef ? GetComponentLogMetadata.apply
          case _         => pekkoLocation.componentRef ? GetComponentLogMetadata.apply
        }
      })
  }

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Done] = {
    val prefix = componentId.prefix

    findComponent(componentId)
      .map(pekkoLocation => {
        log.info(
          s"Setting log level to $level",
          Map("prefix" -> prefix.toString, "location" -> pekkoLocation.toString)
        )
        componentId.componentType match {
          case Sequencer => pekkoLocation.sequencerRef ! SetComponentLogLevel(level)
          case _         => pekkoLocation.componentRef ! SetComponentLogLevel(level)
        }
        Done
      })
  }

}
