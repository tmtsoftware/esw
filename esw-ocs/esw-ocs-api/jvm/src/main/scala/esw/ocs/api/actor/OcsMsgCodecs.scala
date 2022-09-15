/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.api.actor

import csw.command.client.cbor.MessageCodecs
import csw.command.client.messages.sequencer.SequencerMsg
import csw.prefix.codecs.CommonCodecs
import esw.ocs.api.actor.messages.SequencerMessages.EswSequencerRemoteMessage
import esw.ocs.api.actor.messages.{InternalSequencerState, SequenceComponentRemoteMsg}
import esw.ocs.api.codecs.OcsCodecs
import io.bullet.borer.Codec
import io.bullet.borer.derivation.MapBasedCodecs.deriveAllCodecs

/*
 * The codecs of sequencer and sequence components remote messages
 * */
trait OcsMsgCodecs extends MessageCodecs with CommonCodecs with OcsCodecs {
  implicit lazy val eswSequencerMessageCodec: Codec[EswSequencerRemoteMessage]               = deriveAllCodecs
  implicit lazy val sequenceComponentMsgCodec: Codec[SequenceComponentRemoteMsg]             = deriveAllCodecs
  implicit lazy val sequencerBehaviorStateCodec: Codec[InternalSequencerState[SequencerMsg]] = deriveAllCodecs
}
