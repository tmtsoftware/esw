package esw.demo

import akka.actor.typed.scaladsl.ActorContext
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.internal.wiring.{FrameworkWiring, Standalone}
import csw.framework.models.CswContext
import csw.framework.scaladsl.{ComponentBehaviorFactory, ComponentHandlers}
import csw.location.api.models.TrackingEvent
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.params.commands.CommandResponse.{Accepted, Completed, Started}
import csw.params.commands.{CommandResponse, ControlCommand}
import csw.params.core.generics.KeyType.IntKey
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.jdk.DurationConverters.ScalaDurationOps

/**
 * *********************************************
 * 1. Start csw-services
 * 2. Start gateway with metrics
 * 3. Start hcd
 * 4. http POST http://localhost:7654/post-endpoint _type=ListEntries
 * 5. http://localhost:8090/metrics
 *
 * // =========== Event Service (HTTP)==========
 * 1. Get
 * 2. Publish
 *
 * // =========== Command Service (HTTP)==========
 * 1. Validate
 * 2. Submit (immediate)
 * 3. Query
 *
 * // =========== Event Service (WS)==========
 * 1. Subscribe
 * 2. Pattern Subscribe
 *
 * // =========== Command Service (WS)==========
 * 1. Submit (long) then Query Final
 * 2. Oneway then Subscribe current state
 *
 * *********************************************************
 */
object Main extends App {

  private val wiring = new FrameworkWiring()
  LoggingSystemFactory.forTestingOnly()(wiring.actorSystem)

  Await.result(Standalone.spawn(ConfigFactory.parseResources("demoHcd.conf"), wiring), 20.seconds)
}

class HcdBehaviourFactory extends ComponentBehaviorFactory {
  override protected def handlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext): ComponentHandlers =
    new SampleHcdHandlers(ctx, cswCtx)
}

class SampleHcdHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext) extends ComponentHandlers(ctx, cswCtx) {
  import cswCtx._
  private val prefix    = Prefix("IRIS.filter.wheel")
  private val tempState = CurrentState(prefix, StateName("temp"))
  private val tempKey   = IntKey.make("tempKey")

  override def initialize(): Unit = {}

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ???

  override def validateCommand(runId: Id, controlCommand: ControlCommand): CommandResponse.ValidateCommandResponse = {
    println(s"======== validateCommand handler called, command name: ${controlCommand.commandName.name} ========")
    Accepted(runId)
  }

  override def onSubmit(runId: Id, controlCommand: ControlCommand): CommandResponse.SubmitResponse = {
    val commandName = controlCommand.commandName.name
    println(s"======== onSubmit handler called, commandName = $commandName ========")
    commandName match {
      case "long"      => completeAfter(runId, 1.minute); Started(runId)
      case "immediate" => Completed(runId)
      case _           => CommandResponse.Error(runId, s"Unknown command $commandName received!")
    }

  }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {
    val commandName = controlCommand.commandName.name
    println(s"======== onOneway handler called, commandName = $commandName ========")
    commandName match {
      case "temp" => publishStateEvery(5.seconds)
      case _      => CommandResponse.Error(runId, s"Unknown command $commandName received!")
    }
  }

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ???

  override def onOperationsMode(): Unit = ???

  override def onShutdown(): Unit = ???

  override def onGoOffline(): Unit = ???

  override def onGoOnline(): Unit = ???

  private def completeAfter(runId: Id, duration: FiniteDuration) =
    timeServiceScheduler.scheduleOnce(UTCTime.after(duration)) {
      println(s"==== Finishing long running command having runId: $runId ====")
      commandResponseManager.updateCommand(Completed(runId))
    }

  private def publishStateEvery(duration: FiniteDuration) =
    timeServiceScheduler.schedulePeriodically(duration.toJava) {
      println("==== Publishing temp state ====")
      currentStatePublisher.publish(tempState.add(tempKey.set(10)))
    }

}
