package esw.ocs.api

import csw.params.commands.CommandResponse.SubmitResponse

import scala.concurrent.Future

trait SequencerCommandApi {
  def queryFinal: Future[SubmitResponse]
}
