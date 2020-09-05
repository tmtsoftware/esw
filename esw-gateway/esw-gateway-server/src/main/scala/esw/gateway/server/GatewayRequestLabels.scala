package esw.gateway.server

import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest.{ComponentCommand, SequencerCommand}
import msocket.jvm.metrics.Labelled

object GatewayRequestLabels extends GatewayRequestLabels

trait GatewayRequestLabels {
  private val commandMsgLabelName   = "command_msg"
  private val sequencerMsgLabelName = "sequencer_msg"
  private val labelNames            = List(commandMsgLabelName, sequencerMsgLabelName)

  implicit val postRequestLabelled: Labelled[GatewayRequest] = Labelled.make(labelNames) {
    case ComponentCommand(_, command) => Map(commandMsgLabelName -> createLabel(command))
    case SequencerCommand(_, command) => Map(sequencerMsgLabelName -> createLabel(command))
  }

  private[gateway] def createLabel[A](obj: A): String = {
    val name = obj.getClass.getSimpleName
    if (name.endsWith("$")) name.dropRight(1) else name
  }
}
