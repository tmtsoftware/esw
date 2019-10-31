package esw.ocs.dsl.script

import csw.params.core.models.Id
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{OkOrUnhandledResponse, PullNextResponse}

import scala.concurrent.Future

trait SequenceOperator {
  def pullNext: Future[PullNextResponse]
  def maybeNext: Future[Option[Step]]
  def readyToExecuteNext: Future[OkOrUnhandledResponse]
  def stepSuccess(stepId: Id): Unit
  def stepFailure(stepId: Id, message: String): Unit
}
