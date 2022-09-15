/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.server

import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.{ComponentCommand, SequencerCommand}
import msocket.jvm.metrics.LabelExtractor

object GatewayRequestLabels extends GatewayRequestLabels

trait GatewayRequestLabels {
  private val commandMsgLabelName   = "command_msg"
  private val sequencerMsgLabelName = "sequencer_msg"
  private val labelNames            = List(commandMsgLabelName, sequencerMsgLabelName)

  implicit val postRequestLabelled: LabelExtractor[GatewayRequest] = LabelExtractor.make(labelNames) {
    case ComponentCommand(_, command) => Map(commandMsgLabelName -> LabelExtractor.createLabel(command))
    case SequencerCommand(_, command) => Map(sequencerMsgLabelName -> LabelExtractor.createLabel(command))
  }
}
