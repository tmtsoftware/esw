package esw.gateway.server.testdata

import org.apache.pekko.actor.typed.scaladsl.ActorContext
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.logging.api.scaladsl.Logger
import csw.params.commands.CommandResponse.{Accepted, Completed, Started}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.generics.Key
import csw.params.core.generics.KeyType.StringKey
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.params.events.{EventKey, EventName, SystemEvent}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.Await
import scala.concurrent.duration.DurationLong

object SampleAssemblyHandlers {
  val submitResponseKey: Key[String] = StringKey.make("submitResponse")
  val baseResponseEvent: SystemEvent = SystemEvent(Prefix("tcs.response"), EventName("submit-response"))
  val eventKey: EventKey             = baseResponseEvent.eventKey

  def extractResponse(event: SystemEvent): String = event.paramType(SampleAssemblyHandlers.submitResponseKey).head
}

class SampleAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx.*

  val log: Logger = loggerFactory.getLogger(ctx)
  override def initialize(): Unit = {
    log.info("Initializing Component TLA")
    Thread.sleep(100)
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
        // complete long-running command after 100 millis
        timeServiceScheduler.scheduleOnce(UTCTime.after(100.millis)) {
          publishSubmitResponse("Completed")
          commandResponseManager.updateCommand(Completed(runId))
        }

        publishSubmitResponse("Started")
        Started(runId)
      case _ => Completed(runId)
    }

  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
    val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))
    currentStatePublisher.publish(currentState1)
    currentStatePublisher.publish(currentState2)
    log.info("Invoking Oneway Handler TLA")
  }

  override def onShutdown(): Unit = {
    log.info("Shutting down Component TLA")
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

  private def publishSubmitResponse(responseStr: String) = {
    import SampleAssemblyHandlers.*

    Await.result(
      eventService.defaultPublisher.publish(
        baseResponseEvent.add(submitResponseKey.set(responseStr))
      ),
      5.seconds
    )
  }
}
