/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.protocol

import csw.params.commands.CommandIssue.UnsupportedCommandInStateIssue
import csw.params.commands.CommandResponse.{Error, Invalid, SubmitResponse}
import csw.params.core.models.Id
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.{SequencerState, Step, StepList}

/**
 * These models are being used as responses for sequencers e.g., in SequencerApi
 */
sealed trait EswSequencerResponse     extends OcsAkkaSerializable
sealed trait OkOrUnhandledResponse    extends EswSequencerResponse // fixme: think about better name
sealed trait GenericResponse          extends EswSequencerResponse // fixme: think about better name
sealed trait PauseResponse            extends EswSequencerResponse
sealed trait RemoveBreakpointResponse extends EswSequencerResponse
sealed trait PullNextResponse         extends EswSequencerResponse
sealed trait GoOnlineResponse         extends EswSequencerResponse
sealed trait GoOfflineResponse        extends EswSequencerResponse
sealed trait DiagnosticModeResponse   extends EswSequencerResponse
sealed trait OperationsModeResponse   extends EswSequencerResponse

sealed trait SequencerSubmitResponse extends EswSequencerResponse {
  def toSubmitResponse(runId: Id = Id("IdNotAvailable")): SubmitResponse =
    this match {
      case SubmitResult(submitResponse) => submitResponse
      case unhandled: Unhandled         => Invalid(runId, UnsupportedCommandInStateIssue(unhandled.msg))
      case NewSequenceHookFailed(msg)   => Error(runId, msg)
    }
}

sealed trait Ok
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with DiagnosticModeResponse
    with OperationsModeResponse

case object Ok extends Ok

final case class PullNextResult(step: Step)                   extends PullNextResponse
final case class SubmitResult(submitResponse: SubmitResponse) extends SequencerSubmitResponse

final case class Unhandled private[ocs] (state: String, messageType: String, msg: String)
    extends OkOrUnhandledResponse
    with GenericResponse
    with PauseResponse
    with RemoveBreakpointResponse
    with GoOnlineResponse
    with GoOfflineResponse
    with SequencerSubmitResponse
    with PullNextResponse

object Unhandled {
  def apply(state: String, messageType: String): Unhandled =
    new Unhandled(state, messageType, s"Sequencer can not accept '$messageType' message in '$state' state")

  private[ocs] def apply(state: String, messageType: String, description: String): Unhandled = {
    new Unhandled(state, messageType, description)
  }
}

case object GoOnlineHookFailed   extends GoOnlineResponse
case object GoOfflineHookFailed  extends GoOfflineResponse
case object DiagnosticHookFailed extends DiagnosticModeResponse
case object OperationsHookFailed extends OperationsModeResponse

case class NewSequenceHookFailed(msg: String) extends SequencerSubmitResponse

sealed trait EditorError extends GenericResponse

object EditorError {
  case object CannotOperateOnAnInFlightOrFinishedStep extends EditorError with PauseResponse
  final case class IdDoesNotExist(id: Id)             extends EditorError with RemoveBreakpointResponse
}

private[ocs] sealed trait SequencerStateSubscriptionResponse extends EswSequencerResponse
private[ocs] object SequencerStateSubscriptionResponse {
  case object SequencerShuttingDown extends SequencerStateSubscriptionResponse
}
case class SequencerStateResponse(stepList: StepList, sequencerState: SequencerState) extends SequencerStateSubscriptionResponse
