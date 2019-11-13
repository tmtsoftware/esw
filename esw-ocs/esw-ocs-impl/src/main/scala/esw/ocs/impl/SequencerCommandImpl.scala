package esw.ocs.impl

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import esw.ocs.api.SequencerCommandApi
import esw.ocs.api.protocol.{OkOrUnhandledResponse, SequenceResponse}
import esw.ocs.impl.messages.SequencerMessages.{LoadSequence, StartSequence, SubmitSequence}

import scala.concurrent.Future

class SequencerCommandImpl(sequencer: ActorRef[SequencerMsg])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequencerCommandApi {

  import system.executionContext

  // fixme: shouldn't this call have long timeout and not the default?
  override def queryFinal(): Future[SubmitResponse] = sequencer ? QueryFinal

  override def submit(sequence: Sequence): Future[SubmitResponse] = {
    val sequenceResponseF: Future[SequenceResponse] = sequencer ? (SubmitSequence(sequence, _))
    sequenceResponseF.map(_.toSubmitResponse(sequence.runId))
  }

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    sequencer ? (LoadSequence(sequence, _))

  override def startSequence(): Future[SubmitResponse] = {
    val sequenceResponse: Future[SequenceResponse] = sequencer ? StartSequence
    sequenceResponse.map(_.toSubmitResponse())
  }
}
