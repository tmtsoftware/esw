package esw.ocs.api

import csw.params.commands.CommandResponse.{Started, SubmitResponse}
import csw.params.commands.Sequence

import scala.concurrent.{ExecutionContext, Future}

object SequencerCommandApiExt {
  implicit class RichSequencerCommandApi(api: SequencerCommandApi) {
    def submitAndWait(sequence: Sequence)(implicit ec: ExecutionContext): Future[SubmitResponse] = {
      api.submit(sequence).flatMap {
        case Started(_) => api.queryFinal()
        case x          => Future.successful(x)
      }
    }
  }
}
