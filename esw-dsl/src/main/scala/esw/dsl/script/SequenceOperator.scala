package esw.dsl.script

import csw.params.commands.CommandResponse.SubmitResponse
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{OkOrUnhandledResponse, PullNextResponse}

import scala.concurrent.Future

trait SequenceOperator {
  def pullNext: Future[PullNextResponse]
  def maybeNext: Future[Option[Step]]
  def readyToExecuteNext: Future[OkOrUnhandledResponse]
  def update(submitResponse: SubmitResponse): Unit
}
