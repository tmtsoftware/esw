package esw.ocs.core.messages

import csw.command.client.messages.sequencer.SequencerMsg
import esw.ocs.api.models.responses.Unhandled

object UnhandledResponse {
  def apply(state: SequencerState[SequencerMsg], messageType: String): Unhandled =
    new Unhandled(state.entryName, messageType, s"Sequencer can not accept '$messageType' message in '$state' state")

  private[ocs] def apply(state: SequencerState[SequencerMsg], messageType: String, description: String): Unhandled = {
    new Unhandled(state.entryName, messageType, s"Sequencer can not accept '$messageType' message in '$state' state")
  }
}
