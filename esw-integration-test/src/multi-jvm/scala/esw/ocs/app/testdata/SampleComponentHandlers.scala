package esw.ocs.app.testdata

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Prefix
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.UTCTime

import scala.concurrent.Future

class SampleComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  val log: Logger = loggerFactory.getLogger(ctx)
  override def initialize(): Future[Unit] = {
    log.info("Initializing Component TLA")
    Thread.sleep(100)
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse =
    Accepted(controlCommand.runId)

  override def onSubmit(controlCommand: ControlCommand): CommandResponse.SubmitResponse = {
    val event = new SystemEvent(Prefix("tcs.filter.wheel"), EventName("setup-command-from-tcs-sequencer"))
    eventService.defaultPublisher.publish(event)
    Completed(controlCommand.runId)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Future[Unit] = {
    log.info("Shutting down Component TLA")
    Future.unit
  }

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
