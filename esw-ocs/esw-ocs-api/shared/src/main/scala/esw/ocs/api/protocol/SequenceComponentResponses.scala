/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.protocol

import csw.location.api.models.AkkaLocation
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.SequenceComponentState
import esw.ocs.api.protocol.SequenceComponentResponse.ScriptErrorOrSequencerLocation

/*
 * These models are being used as responses for sequence components e.g., in SequenceComponentApi
 */
object SequenceComponentResponse {

  sealed trait ScriptResponseOrUnhandled extends OcsAkkaSerializable
  sealed trait OkOrUnhandled             extends OcsAkkaSerializable

  final case class GetStatusResponse(response: Option[AkkaLocation]) extends OcsAkkaSerializable

  sealed trait ScriptErrorOrSequencerLocation extends ScriptResponseOrUnhandled

  final case object Ok extends OkOrUnhandled

  final case class Unhandled(state: SequenceComponentState, messageType: String, msg: String)
      extends ScriptResponseOrUnhandled
      with OkOrUnhandled

  object Unhandled {
    def apply(state: SequenceComponentState, messageType: String): Unhandled =
      new Unhandled(state, messageType, s"Sequence Component can not accept '$messageType' message in '${state.entryName}'")
  }

  final case class SequencerLocation(location: AkkaLocation) extends ScriptErrorOrSequencerLocation
}

sealed trait ScriptError extends Exception with ScriptErrorOrSequencerLocation {
  def msg: String
  override def getMessage: String = msg
}

object ScriptError {
  case class LocationServiceError(msg: String) extends ScriptError
  case class LoadingScriptFailed(msg: String)  extends ScriptError
}
