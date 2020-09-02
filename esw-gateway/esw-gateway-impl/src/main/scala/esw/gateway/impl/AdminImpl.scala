package esw.gateway.impl

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.{GetComponentLogMetadata, SetComponentLogLevel}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType._
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.scaladsl.LocationService
import csw.logging.api.scaladsl.Logger
import csw.logging.models.{Level, LogMetadata}
import esw.constants.Timeouts
import esw.gateway.api.AdminApi
import esw.gateway.api.protocol.InvalidComponent

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class AdminImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_]) extends AdminApi {
  import actorSystem.executionContext
  private val log: Logger       = AdminLogger.getLogger
  implicit val timeout: Timeout = Timeout(5.seconds)

  override def getLogMetadata(componentId: ComponentId): Future[LogMetadata] = {
    val akkaConnection = AkkaConnection(componentId)
    val prefix         = componentId.prefix

    locationService
      .find(akkaConnection)
      .flatMap(mayBeAkkaLocation => {
        mayBeAkkaLocation
          .map(akkaLocation => {
            log.info(
              "Getting log information from logging system",
              Map("prefix" -> prefix.toString, "location" -> akkaLocation.toString)
            )
            val response: Future[LogMetadata] = componentId.componentType match {
              case Sequencer =>
                (akkaLocation.sequencerRef ? GetComponentLogMetadata)(Timeouts.GetLogMetadata, actorSystem.scheduler)
              case _ => (akkaLocation.componentRef ? GetComponentLogMetadata)(Timeouts.GetLogMetadata, actorSystem.scheduler)
            }
            response
          })
          .getOrElse[Future[LogMetadata]](throw InvalidComponent(s"Could not find component : $componentId"))
      })
  }

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Done] = {
    val akkaConnection = AkkaConnection(componentId)
    val prefix         = componentId.prefix

    locationService
      .find(akkaConnection)
      .map(mayBeAkkaLocation => {
        mayBeAkkaLocation
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
          .getOrElse[Done](throw InvalidComponent(s"Could not find component : $componentId"))
      })
  }
}
