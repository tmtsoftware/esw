package esw.ocs.dsl2.core

import csw.params.commands.SequenceCommand
import esw.ocs.dsl2.highlevel.models.ScriptError
import esw.ocs.dsl.script.CommandHandler
import esw.ocs.dsl2.Extensions.toScriptError
import esw.ocs.dsl2.highlevel.LoopDsl

import java.util.concurrent.CompletionStage
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.{Duration, DurationLong}
import async.Async.*
import scala.jdk.FutureConverters.FutureOps

class CommandHandlerScala[T <: SequenceCommand](block: T => Future[Unit], loopDsl: LoopDsl)(using ExecutionContext)
    extends CommandHandler[T] {
  private var retryCount: Int                              = 0
  private var onError: Option[ScriptError => Future[Unit]] = None
  private var delayInMillis: Long                          = 0

  override def execute(sequenceCommand: T): CompletionStage[Void] = {
    var localRetryCount = retryCount

    def go(): Future[Unit] = async {
      try await(block(sequenceCommand))
      catch
        case ex =>
          onError.foreach(f => await(f(ex.toScriptError)))
          if (localRetryCount > 0)
            localRetryCount -= 1
            loopDsl.delay(delayInMillis.millis)
            await(go())
          else throw ex
    }

    go().map(_ => null).asJava
  }

  def onError(block: ScriptError => Future[Unit]): CommandHandlerScala[T] =
    onError = Some(block)
    this

  def retry(count: Int): Unit =
    retryCount = count

  def retry(count: Int, interval: Duration): Unit = {
    retry(count)
    delayInMillis = interval.toMillis
  }

}
