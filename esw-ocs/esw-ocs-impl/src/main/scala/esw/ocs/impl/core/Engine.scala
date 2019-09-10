package esw.ocs.impl.core

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.params.commands.CommandResponse.Error
import csw.params.core.models.Id
import esw.ocs.api.models.responses.{PullNextResult, Unhandled}
import esw.ocs.impl.dsl.Script

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

private[ocs] class Engine(implicit mat: Materializer) {
  import mat.executionContext

  def start(sequenceOperator: SequenceOperator, script: Script): Future[Done] = {
    Source.repeat(()).mapAsync(1)(_ => processStep(sequenceOperator, script)).runForeach(_ => ())
  }

  /*
    pullNext keeps pulling next pending command
    job of engine is to pull only one command and wait for its completion then pull next
    this is achieved with the combination of pullNext and readyToExecuteNext
   */
  private def processStep(sequenceOperator: SequenceOperator, script: Script): Future[Done] = async {
    val pullNextResponse = await(sequenceOperator.pullNext)

    pullNextResponse match {
      case PullNextResult(step) =>
        script.execute(step.command).recover {
          case NonFatal(e) => sequenceOperator.update(Error(step.id, e.getMessage))
        }
      case e: Unhandled => sequenceOperator.update(Error(Id("Invalid"), e.msg))
    }

    await(sequenceOperator.readyToExecuteNext)
    Done
  }
}
