package csw.framework.testkit

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

private[testkit] class TestComponentHandlersAdapter(
    ctx: ActorContext[TopLevelActorMessage],
    cswCtx: CswContext,
    testHandlers: TestComponentHandlers
) extends ComponentHandlers(ctx, cswCtx) {
  override def initialize(): Unit = testHandlers.initialize(cswCtx)

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit =
    testHandlers.onLocationTrackingEvent(cswCtx, trackingEvent)

  override def validateCommand(runId: Id, controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse =
    testHandlers.validateCommand(cswCtx, runId, controlCommand)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): CommandResponse.SubmitResponse =
    testHandlers.onSubmit(cswCtx, runId, controlCommand)

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit =
    testHandlers.onOneway(cswCtx, runId, controlCommand)

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit =
    testHandlers.onDiagnosticMode(cswCtx, startTime, hint)

  override def onOperationsMode(): Unit = testHandlers.onOperationsMode(cswCtx)

  override def onShutdown(): Unit = testHandlers.onShutdown(cswCtx)

  override def onGoOffline(): Unit = testHandlers.onGoOffline(cswCtx)

  override def onGoOnline(): Unit = testHandlers.onGoOnline(cswCtx)
}
