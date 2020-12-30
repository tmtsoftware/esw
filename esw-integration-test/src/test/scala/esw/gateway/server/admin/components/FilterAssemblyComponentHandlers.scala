package esw.gateway.server.admin.components

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Completed, SubmitResponse, ValidateCommandResponse}
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.time.core.models.UTCTime

class FilterAssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  val log: Logger = cswCtx.loggerFactory.getLogger

  import esw.gateway.server.admin.SampleContainerState._

  override def initialize(): Unit = {
    cswCtx.currentStatePublisher.publish(
      CurrentState(eswAssemblyPrefix, StateName("Initializing_Filter_Assembly"), Set(choiceKey.set(initChoice)))
    )
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ()

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = Completed(runId)

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ()

  override def onShutdown(): Unit = {
    cswCtx.currentStatePublisher.publish(
      CurrentState(eswAssemblyPrefix, StateName("Shutdown_Filter_Assembly"), Set(choiceKey.set(shutdownChoice)))
    )
  }

  override def onGoOffline(): Unit = ()

  override def onGoOnline(): Unit = ()

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ()

  override def onOperationsMode(): Unit = ()
}
