package esw.performance.components

import akka.actor.typed.scaladsl.ActorContext
import csw.command.api.scaladsl.CommandService
import csw.command.client.CommandServiceFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType, TrackingEvent}
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

class SimpleAssemblyHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx.*
  private val log = loggerFactory.getLogger

  private val connection: AkkaConnection = AkkaConnection(ComponentId(Prefix("CSW.sampleHcd"), ComponentType.HCD))
  private var akkaLocation: AkkaLocation = _

  override def initialize(): Unit = {
    log.info("Initializing sampleAssembly...")
    akkaLocation = Await.result(locationService.resolve(connection, 10.seconds), 10.seconds).get
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse = {
    val hcdComponent: CommandService = CommandServiceFactory.make(akkaLocation)(ctx.system)
    val hcdResponse                  = hcdComponent.submitAndWait(controlCommand)(60.seconds)
    Await.result(hcdResponse, 60.seconds)
  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}

}
