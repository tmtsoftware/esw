package esw.agent.test

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.ExecutionContextExecutor

class TestAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx.*
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  override def initialize(): Unit = {
    log.info("Initializing sampleAssembly...")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = Completed(runId)

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

}
