package esw.ocs.impl.core

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import esw.ocs.api.protocol.{PullNextResult, Unhandled}
import esw.ocs.dsl.script.{JScriptDsl, SequenceOperator}

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

private[ocs] class Engine(script: JScriptDsl) {

  def start(sequenceOperator: SequenceOperator)(implicit mat: Materializer): Future[Done] = {
    Source.repeat(()).mapAsync(1)(_ => processStep(sequenceOperator)).runForeach(_ => ())
  }

  /*
    pullNext keeps pulling next pending command
    job of engine is to pull only one command and wait for its completion then pull next
    this is achieved with the combination of pullNext and readyToExecuteNext
   */
  private def processStep(sequenceOperator: SequenceOperator)(implicit mat: Materializer): Future[Done] =
    async {
      import mat.executionContext
      val pullNextResponse = await(sequenceOperator.pullNext)

      pullNextResponse match {
        case PullNextResult(step) =>
          script.execute(step.command).recover {
            case NonFatal(e) => sequenceOperator.stepFailure(e.getMessage)
          }
        case _: Unhandled =>
      }

      await(sequenceOperator.readyToExecuteNext)
      Done
    }(mat.executionContext)
}
