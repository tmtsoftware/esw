package esw.ocs.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.protocol.SequenceComponentResponse._

sealed trait SequenceComponentMsg
sealed trait SequenceComponentRemoteMsg extends SequenceComponentMsg with OcsAkkaSerializable

sealed trait UnhandleableSequenceComponentMsg extends SequenceComponentMsg {
  def replyTo: ActorRef[Unhandled]
}

sealed trait IdleStateSequenceComponentMsg    extends SequenceComponentMsg
sealed trait RunningStateSequenceComponentMsg extends SequenceComponentMsg

object SequenceComponentMsg {
  final case class LoadScript(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[ScriptResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with IdleStateSequenceComponentMsg

  final case class UnloadScript(replyTo: ActorRef[Ok.type])
      extends SequenceComponentRemoteMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  final case class Restart(replyTo: ActorRef[ScriptResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  final case class GetStatus(replyTo: ActorRef[GetStatusResponse])
      extends SequenceComponentRemoteMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  final case class Shutdown(replyTo: ActorRef[Ok.type])
      extends SequenceComponentRemoteMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  private[ocs] final case object Stop
      extends SequenceComponentMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg
}
