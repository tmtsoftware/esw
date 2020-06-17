package esw.ocs.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.protocol.SequenceComponentResponse.{
  GetStatusResponseOrUnhandled,
  OkOrUnhandled,
  ScriptResponseOrUnhandled,
  Unhandled
}

sealed trait SequenceComponentMsg
sealed trait SequenceComponentRemoteMsg extends SequenceComponentMsg with OcsAkkaSerializable

sealed trait UnhandleableSequenceComponentMsg extends SequenceComponentMsg {
  def replyTo: ActorRef[Unhandled]
}

sealed trait IdleStateSequenceComponentMsg         extends SequenceComponentMsg
sealed trait RunningStateSequenceComponentMsg      extends SequenceComponentMsg
sealed trait ShuttingDownStateSequenceComponentMsg extends SequenceComponentMsg

object SequenceComponentMsg {
  final case class LoadScript(subsystem: Subsystem, observingMode: String, replyTo: ActorRef[ScriptResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with IdleStateSequenceComponentMsg

  final case class UnloadScript(replyTo: ActorRef[OkOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  final case class Restart(replyTo: ActorRef[ScriptResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  final case class GetStatus(replyTo: ActorRef[GetStatusResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  final case class Shutdown(replyTo: ActorRef[OkOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  private[ocs] final case class ShutdownInternal(replyTo: ActorRef[OkOrUnhandled])
      extends UnhandleableSequenceComponentMsg
      with ShuttingDownStateSequenceComponentMsg

  private[ocs] final case object Stop
      extends SequenceComponentMsg
      with IdleStateSequenceComponentMsg
      with RunningStateSequenceComponentMsg
}
