package esw.gateway.server.testdata

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Completed, Error, Started}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.params.events.{EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.Future

class SampleHcdHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  val log: Logger = loggerFactory.getLogger(ctx)
  override def initialize(): Future[Unit] = {
    log.info("Initializing Component TLA")
    Thread.sleep(100)
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse =
    Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): CommandResponse.SubmitResponse = {
    log.info(s"Run Id is $runId")
    val event = new SystemEvent(Prefix("tcs.filter.wheel"), EventName("setup-command-from-script"))
    eventService.defaultPublisher.publish(event)

    controlCommand.commandName.name match {
      case "long-running" =>
        commandResponseManager.updateCommand(Completed(runId))
        Started(runId)
      case _ => Error(runId, "command-failed")
    }

  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = {
    log.info("Shutting down Component TLA")
    Future.unit
  }

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???
}
