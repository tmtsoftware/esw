package esw.ocs.impl

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.core.models.Id
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol._
import esw.ocs.api.{SequencerCommandApi, SequencerCommandExtensions}
import esw.ocs.impl.messages.SequencerMessages._

import scala.concurrent.Future

class SequencerCommandImpl(sequencer: ActorRef[SequencerMsg])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequencerCommandApi {

  import system.executionContext

  private val extensions = new SequencerCommandExtensions(this)

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] =
    sequencer ? (LoadSequence(sequence, _))

  override def startSequence(): Future[SubmitResponse] = {
    val sequenceResponse: Future[SequenceResponse] = sequencer ? StartSequence
    sequenceResponse.map(_.toSubmitResponse())
  }

  override def submit(sequence: Sequence): Future[SubmitResponse] = {
    val sequenceResponseF: Future[SequenceResponse] = sequencer ? (SubmitSequence(sequence, _))
    sequenceResponseF.map(_.toSubmitResponse())
  }

  override def submitAndWait(sequence: Sequence): Future[SubmitResponse] = extensions.submitAndWait(sequence)

  // fixme: shouldn't this call have long timeout and not the default?
  override def queryFinal(runId: Id): Future[SubmitResponse] = sequencer ? (QueryFinal(runId, _))

  override def goOnline(): Future[GoOnlineResponse] = sequencer ? GoOnline

  override def goOffline(): Future[GoOfflineResponse] = sequencer ? GoOffline

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] =
    sequencer ? (DiagnosticMode(startTime, hint, _))

  override def operationsMode(): Future[OperationsModeResponse] = sequencer ? OperationsMode

}
