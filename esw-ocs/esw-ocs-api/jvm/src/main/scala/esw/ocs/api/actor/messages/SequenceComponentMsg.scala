package esw.ocs.api.actor.messages

import akka.actor.typed.ActorRef
import csw.prefix.models.Subsystem
import esw.ocs.api.codecs.OcsAkkaSerializable
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.*

/*
 * These are messages(models) of sequence component actor.
 * These are being used to communication with the sequence component actor
 * and in the implementation of it.
 */
sealed trait SequenceComponentMsg
sealed trait SequenceComponentRemoteMsg extends SequenceComponentMsg with OcsAkkaSerializable

sealed trait UnhandleableSequenceComponentMsg extends SequenceComponentMsg {
  def replyTo: ActorRef[Unhandled]
}

sealed trait IdleStateSequenceComponentMsg    extends SequenceComponentMsg
sealed trait RunningStateSequenceComponentMsg extends SequenceComponentMsg
sealed trait CommonMsg                        extends IdleStateSequenceComponentMsg with RunningStateSequenceComponentMsg

object SequenceComponentMsg {
  final case class UnloadScript(replyTo: ActorRef[Ok.type]) extends SequenceComponentRemoteMsg with CommonMsg

  final case class GetStatus(replyTo: ActorRef[GetStatusResponse]) extends SequenceComponentRemoteMsg with CommonMsg

  final case class Shutdown(replyTo: ActorRef[Ok.type]) extends SequenceComponentRemoteMsg with CommonMsg

  final case class LoadScript(subsystem: Subsystem, obsMode: ObsMode, replyTo: ActorRef[ScriptResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with IdleStateSequenceComponentMsg

  final case class RestartScript(replyTo: ActorRef[ScriptResponseOrUnhandled])
      extends SequenceComponentRemoteMsg
      with UnhandleableSequenceComponentMsg
      with RunningStateSequenceComponentMsg

  private[ocs] final case object Stop extends SequenceComponentMsg with CommonMsg
}
