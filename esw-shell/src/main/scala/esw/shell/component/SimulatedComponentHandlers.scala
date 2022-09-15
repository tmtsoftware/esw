/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.shell.component

import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import csw.command.client.messages.TopLevelActorMessage
import csw.framework.models.CswContext
import csw.framework.scaladsl.ComponentHandlers
import csw.location.api.models.TrackingEvent
import csw.params.commands.CommandIssue.UnsupportedCommandIssue
import csw.params.commands.CommandResponse._
import csw.params.commands._
import csw.params.core.generics.KeyType
import csw.params.core.models.Id
import csw.time.core.models.UTCTime

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.{FiniteDuration, MILLISECONDS}

// ComponentHandlers of the simulated HCDs/Assemblies
class SimulatedComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  import cswCtx._
  implicit val ec: ExecutionContextExecutor = ctx.executionContext
  private val log                           = loggerFactory.getLogger

  sealed trait SleepCommand
  case class Sleep(runId: Id, timeInMillis: Long) extends SleepCommand

  private val workerActor =
    ctx.spawn(
      Behaviors.receiveMessage[SleepCommand](msg => {
        msg match {
          case sleep: Sleep =>
            log.trace(s"WorkerActor received sleep command with time of ${sleep.timeInMillis} ms")
            // simulate long running command
            val when: UTCTime = UTCTime.after(FiniteDuration(sleep.timeInMillis, MILLISECONDS))
            timeServiceScheduler.scheduleOnce(when) {
              commandResponseManager.updateCommand(CommandResponse.Completed(sleep.runId))
            }
          case _ => log.error("Unsupported message type")
        }
        Behaviors.same
      }),
      "WorkerActor"
    )

  private val sleepTimeKey = KeyType.LongKey.make("timeInMs")

  override def initialize(): Unit = {
    log.info("Initializing Simple Assembly...")
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = {}

  def onSetup(runId: Id, command: Setup): SubmitResponse = {
    command.commandName match {
      case CommandName("noop") =>
        Completed(runId)
      case CommandName("sleep") =>
        workerActor ! Sleep(runId, command(sleepTimeKey).head)
        Started(runId)
      case _ =>
        Invalid(runId, UnsupportedCommandIssue("command not supported"))
    }
  }

  def onObserve(runId: Id, command: Observe): SubmitResponse = {
    command.commandName match {
      case CommandName("noop") =>
        Completed(runId)
      case _ =>
        Invalid(runId, UnsupportedCommandIssue("command not supported"))
    }
  }

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse =
    controlCommand match {
      case s: Setup   => onSetup(runId, s)
      case o: Observe => onObserve(runId, o)
    }

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = {}

  override def onShutdown(): Unit = {}

  override def onGoOffline(): Unit = {}

  override def onGoOnline(): Unit = {}

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = {}

  override def onOperationsMode(): Unit = {}
}
