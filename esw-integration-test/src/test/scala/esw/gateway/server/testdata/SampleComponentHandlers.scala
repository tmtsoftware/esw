package esw.gateway.server.testdata

import akka.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Completed}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.Prefix
import csw.params.core.states.{CurrentState, StateName}
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

  override def onSubmit(controlCommand: ControlCommand): CommandResponse.SubmitResponse =
    Completed(controlCommand.runId)

  override def onOneway(controlCommand: ControlCommand): Unit = {
    val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
    val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))
    currentStatePublisher.publish(currentState1)
    currentStatePublisher.publish(currentState2)
    log.info("Invoking Oneway Handler TLA")
  }

  override def onShutdown(): Future[Unit] = {
    log.info("Shutting down Component TLA")
    Future.unit
  }

  override def onGoOffline(): Unit = {
    val event = new SystemEvent(Prefix("tcs.filter.wheel"), EventName("offline"))
    eventService.defaultPublisher.publish(event)
  }

  override def onGoOnline(): Unit = {
    val event = new SystemEvent(Prefix("tcs.filter.wheel"), EventName("online"))
    eventService.defaultPublisher.publish(event)
  }

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {
    val diagnosticModeParam = StringKey.make("mode").set("diagnostic")
    val event               = SystemEvent(Prefix("tcs.filter.wheel"), EventName("diagnostic-data")).add(diagnosticModeParam)
    eventService.defaultPublisher.publish(event)
  }

  override def onOperationsMode(): Unit = {
    val operationsModeParam = StringKey.make("mode").set("operations")
    val event               = SystemEvent(Prefix("tcs.filter.wheel"), EventName("diagnostic-data")).add(operationsModeParam)
    eventService.defaultPublisher.publish(event)
  }
}
