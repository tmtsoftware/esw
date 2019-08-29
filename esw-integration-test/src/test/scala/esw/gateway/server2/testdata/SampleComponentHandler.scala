package esw.gateway.server2.testdata

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Started}
import csw.params.commands.{CommandResponse, ControlCommand}

import scala.concurrent.Future

class SampleComponentHandler(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._

  val log: Logger = loggerFactory.getLogger(ctx)
  override def initialize(): Future[Unit] = {
    log.info("Initializing Component TLA")
    Thread.sleep(100)
    Future.unit
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse = {
    Accepted(controlCommand.runId)
  }

  override def onSubmit(controlCommand: ControlCommand): CommandResponse.SubmitResponse = {
    Started(controlCommand.runId)
  }

  override def onOneway(controlCommand: ControlCommand): Unit = ???

  override def onShutdown(): Future[Unit] = {
    log.info("Shutting down Component TLA")
    Future.unit
  }

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???
}
