package esw.gateway.impl

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.{Askable, _}
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.RunningMessage.Lifecycle
import csw.command.client.messages.SupervisorContainerCommonMessages.{Restart, Shutdown}
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.command.client.models.framework.ToComponentLifecycleMessage.{GoOffline, GoOnline}
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.logging.models.{Level, LogMetadata}
import esw.constants.AdminTimeouts
import esw.gateway.api.AdminApi
import esw.gateway.api.protocol.InvalidComponent

import scala.concurrent.Future

class AdminImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AdminApi {
  import actorSystem.executionContext
  private val log: Logger       = AdminLogger.getLogger
  implicit val timeout: Timeout = AdminTimeouts.GetLogMetadata

  private def findComponent(componentId: ComponentId): Future[AkkaLocation] =
    locationService
      .find(AkkaConnection(componentId))
      .map(_.getOrElse(throw InvalidComponent(s"Could not find component : $componentId")))

  override def shutdown(componentId: ComponentId): Future[Done] = {
    findComponent(componentId).map(akkaLocation => {
      componentId.componentType match {
        case Container => akkaLocation.containerRef ! Shutdown
        case _         => akkaLocation.componentRef ! Shutdown
      }
      Done
    })
  }

  override def restart(componentId: ComponentId): Future[Done] = {
    findComponent(componentId).map(akkaLocation => {
      componentId.componentType match {
        case Container => akkaLocation.containerRef ! Restart
        case _         => akkaLocation.componentRef ! Restart
      }
      Done
    })
  }

  override def goOffline(componentId: ComponentId): Future[Done] = {
    findComponent(componentId).map(akkaLocation => {
      componentId.componentType match {
        case Container => akkaLocation.containerRef ! Lifecycle(GoOffline)
        case _         => akkaLocation.componentRef ! Lifecycle(GoOffline)
      }
      Done
    })
  }

  override def goOnline(componentId: ComponentId): Future[Done] = {
    findComponent(componentId).map(akkaLocation => {
      componentId.componentType match {
        case Container => akkaLocation.containerRef ! Lifecycle(GoOnline)
        case _         => akkaLocation.componentRef ! Lifecycle(GoOnline)
      }
      Done
    })
  }

  override def getLogMetadata(componentId: ComponentId): Future[LogMetadata] = {
    val prefix = componentId.prefix

    findComponent(componentId)
      .flatMap(akkaLocation => {
        log.info(
          "Getting log information from logging system",
          Map("prefix" -> prefix.toString, "location" -> akkaLocation.toString)
        )
        componentId.componentType match {
          case Sequencer => akkaLocation.sequencerRef ? GetComponentLogMetadata
          case _         => akkaLocation.componentRef ? GetComponentLogMetadata
        }
      })
  }

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Done] = {
    val prefix = componentId.prefix

    findComponent(componentId)
      .map(akkaLocation => {
        log.info(
          s"Setting log level to $level",
          Map("prefix" -> prefix.toString, "location" -> akkaLocation.toString)
        )
        componentId.componentType match {
          case Sequencer => akkaLocation.sequencerRef ! SetComponentLogLevel(level)
          case _         => akkaLocation.componentRef ! SetComponentLogLevel(level)
        }
        Done
      })
  }

}
