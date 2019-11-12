package esw.ocs.impl

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SequencerMsg.QueryFinal
import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.SequencerCommandApi

import scala.concurrent.Future

class SequencerCommandImpl(sequencer: ActorRef[SequencerMsg])(implicit system: ActorSystem[_], timeout: Timeout)
    extends SequencerCommandApi {

  // fixme: shouldn't this call have long timeout and not the default?
  override def queryFinal: Future[SubmitResponse] = sequencer ? QueryFinal
}
