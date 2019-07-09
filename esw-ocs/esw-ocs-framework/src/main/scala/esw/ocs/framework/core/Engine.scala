package esw.ocs.framework.core

import akka.Done
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import csw.params.commands.CommandResponse.Error
import esw.ocs.framework.dsl.Script

import scala.async.Async._
import scala.concurrent.Future
import scala.util.control.NonFatal

class Engine(implicit mat: Materializer) {
  import mat.executionContext

  def start(sequenceOperator: SequenceOperator, script: Script): Future[Done] = {
    Source.repeat(()).mapAsync(1)(_ => processStep(sequenceOperator, script)).runForeach(_ => ())
  }

  /*
    pullNext keeps pulling next pending command
    job of engine is to pull only one command and wait for its completion then pull next
    this is achieved with the combination of pullNext and readyToExecuteNext
   */
  def processStep(sequenceOperator: SequenceOperator, script: Script): Future[Done] = async {
    val step = await(sequenceOperator.pullNext)
    //todo: what happens when execute handler fails?
    script.execute(step.command).recover {
      case NonFatal(e) ⇒
        e.printStackTrace()
        sequenceOperator.update(Error(step.command.runId, e.getMessage))
    }

    await(sequenceOperator.readyToExecuteNext)
  }
}
