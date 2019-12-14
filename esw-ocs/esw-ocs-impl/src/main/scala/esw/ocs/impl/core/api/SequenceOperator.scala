package esw.ocs.impl.core.api

import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{OkOrUnhandledResponse, PullNextResponse}

import scala.concurrent.Future

// todo : why is this needed, as sequence operator can be used instead.
//  Making constructor of SequenceOperatorImpl private to OCS should suffice the need.
trait SequenceOperator {
  def pullNext: Future[PullNextResponse]
  def maybeNext: Future[Option[Step]]
  def readyToExecuteNext: Future[OkOrUnhandledResponse]
  def stepSuccess(): Unit
  def stepFailure(message: String): Unit
}
