package esw.ocs.api

import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import esw.ocs.api.protocol.OkOrUnhandledResponse

import scala.concurrent.Future

trait SequencerCommandApi {
  def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse]
  def startSequence(): Future[SubmitResponse]
  def submit(sequence: Sequence): Future[SubmitResponse]
//  def submitAndWait(sequence: Sequence): Future[SubmitResponse]
  def queryFinal(): Future[SubmitResponse]
}
