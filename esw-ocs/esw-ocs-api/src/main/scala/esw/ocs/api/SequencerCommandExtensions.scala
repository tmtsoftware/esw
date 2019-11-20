package esw.ocs.api

import csw.params.commands.CommandResponse.{Started, SubmitResponse}
import csw.params.commands.Sequence

import scala.concurrent.{ExecutionContext, Future}

class SequencerCommandExtensions(sequencerCommandApi: SequencerCommandApi)(implicit ec: ExecutionContext) {
  def submitAndWait(sequence: Sequence): Future[SubmitResponse] = {
    sequencerCommandApi.submit(sequence).flatMap {
      case Started(runId) => sequencerCommandApi.queryFinal(runId)
      case x              => Future.successful(x)
    }
  }
}
