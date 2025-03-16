package esw.ocs.impl.core

import org.apache.pekko.Done
import org.apache.pekko.stream.Materializer
import org.apache.pekko.stream.scaladsl.Source
import esw.ocs.api.protocol.{PullNextResult, Unhandled}
import esw.ocs.impl.script.ScriptApi

import cps.compat.FutureAsync.*
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

private[ocs] class Engine(script: ScriptApi) {

  def start(sequenceOperator: SequenceOperator)(implicit mat: Materializer): Future[Done] = {
    Source.repeat(()).mapAsync(1)(_ => processStep(sequenceOperator)(mat.executionContext)).runForeach(_ => ())
  }

  /*
     Flow of Step execution
       While starting, the pullNext future will wait till any Sequence is loaded and started in the sequencer. Once sequence is started,
       a Step will complete the future. Engine will execute the Step by executing the respective script handlers, and will update the
       result of the execution of the as Step success or failure in the sequencer. It will wait on the readToExecuteNext future.
       - In case of the successful execution of the Step, the Sequencer will mark the Step as Success.
       - In case of failure of Step, the Step will be marked as Failure, and the Sequence will complete with Error.

       Post Step status updation, `readyToExecuteNext` future will be completed which will result into Getting back into the loop (started in `start`
       method using Source.repeat) and wait for the next Step.
   */
  private def processStep(sequenceOperator: SequenceOperator)(implicit ec: ExecutionContext): Future[Done] =
    async {
      val pullNextResponse = await(sequenceOperator.pullNext)

      pullNextResponse match {
        case PullNextResult(step) =>
          script.execute(step.command).onComplete {
            case _: Success[_] =>
              sequenceOperator.stepSuccess()
            case Failure(e) =>
              val message = if (e.getCause != null) e.getCause.getMessage else e.getMessage
              sequenceOperator.stepFailure(message)
          }
        case _: Unhandled =>
      }

      await(sequenceOperator.readyToExecuteNext)
      Done
    }
}
