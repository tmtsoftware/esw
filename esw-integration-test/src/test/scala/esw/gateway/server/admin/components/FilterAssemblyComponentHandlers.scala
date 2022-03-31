/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

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
import csw.time.core.models.UTCTime

class FilterAssemblyComponentHandlers(ctx: ActorContext[TopLevelActorMessage], cswCtx: CswContext)
    extends ComponentHandlers(ctx, cswCtx) {

  val log: Logger = cswCtx.loggerFactory.getLogger

  import esw.gateway.server.admin.SampleContainerState._

  override def initialize(): Unit = {
    cswCtx.currentStatePublisher.publish(assemblyInitializeCurrentState)
  }

  override def onLocationTrackingEvent(trackingEvent: TrackingEvent): Unit = ()

  override def validateCommand(runId: Id, controlCommand: ControlCommand): ValidateCommandResponse = Accepted(runId)

  override def onSubmit(runId: Id, controlCommand: ControlCommand): SubmitResponse =
    Completed(runId)

  override def onOneway(runId: Id, controlCommand: ControlCommand): Unit = ()

  override def onShutdown(): Unit = {
    cswCtx.currentStatePublisher.publish(assemblyShutdownCurrentState)
  }

  override def onGoOffline(): Unit = ()

  override def onGoOnline(): Unit = ()

  override def onDiagnosticMode(startTime: UTCTime, hint: String): Unit = ()

  override def onOperationsMode(): Unit = ()
}
