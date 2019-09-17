package esw.ocs.impl.dsl.javadsl

import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.Supplier

import akka.Done
import csw.params.commands.{Observe, SequenceCommand, Setup}
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.PullNextResult
import esw.ocs.impl.dsl.Async._
import esw.ocs.impl.dsl.utils.{FunctionBuilder, FunctionHandlers}
import esw.ocs.impl.dsl.{BaseScriptDsl, CswServices}
import esw.ocs.impl.exceptions.UnhandledCommandException
import esw.ocs.macros.StrandEc

import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class JScript(val csw: CswServices) extends BaseScriptDsl {

  protected implicit def strandEc: StrandEc
  protected implicit lazy val toEc: ExecutionContext = strandEc.ec

  var isOnline = true

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, CompletionStage[Void]] = new FunctionBuilder

  private val onlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]   = new FunctionHandlers
  private val offlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]  = new FunctionHandlers
  private val shutdownHandlers: FunctionHandlers[Unit, CompletionStage[Void]] = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, CompletionStage[Void]]    = new FunctionHandlers

  private[esw] def merge(that: JScript): JScript = {
    commandHandlerBuilder ++ that.commandHandlerBuilder
    onlineHandlers ++ that.onlineHandlers
    offlineHandlers ++ that.offlineHandlers
    shutdownHandlers ++ that.shutdownHandlers
    abortHandlers ++ that.abortHandlers
    this
  }

  private def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => CompletionStage[Void]): Unit =
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)

  private lazy val commandHandler: SequenceCommand => CompletionStage[Void] =
    commandHandlerBuilder.build { input =>
      // fixme: should script writer have ability to add this default handler, like handleUnknownCommand
      CompletableFuture.failedFuture(new UnhandledCommandException(input))
    }

  private[ocs] def execute(command: SequenceCommand): Future[Unit] = commandHandler(command).toScala.map(_ => ())

  private def executeHandler[T, S](f: FunctionHandlers[Unit, CompletionStage[Void]]): Future[Unit] =
    Future.sequence(f.execute(()).map(_.toScala)).map(_ => ())

  private[ocs] def executeGoOnline(): Future[Done] =
    executeHandler(onlineHandlers).map { _ =>
      isOnline = true
      Done
    }

  private[ocs] def executeGoOffline(): Future[Done] = {
    isOnline = false
    executeHandler(offlineHandlers).map(_ => Done)
  }

  private[ocs] def executeShutdown(): Future[Done] = executeHandler(shutdownHandlers).map(_ => Done)

  private[ocs] def executeAbort(): Future[Done] = executeHandler(abortHandlers).map(_ => Done)

  // todo:
  private[ocs] def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] = ???
  private[ocs] def executeOperationsMode(): Future[Done]                                 = ???

  protected final def jNextIf(f: SequenceCommand => Boolean): CompletionStage[Optional[SequenceCommand]] =
    async {
      val operator  = csw.sequenceOperatorFactory()
      val mayBeNext = await(operator.maybeNext)
      mayBeNext match {
        case Some(step) if f(step.command) =>
          await(operator.pullNext) match {
            case PullNextResult(step) => Optional.ofNullable(step.command)
            case _                    => Optional.empty[SequenceCommand]
          }
        case _ => Optional.empty[SequenceCommand]
      }
    }.toJava

  protected final def jHandleSetupCommand(name: String)(handler: Setup => CompletionStage[Void]): Unit =
    handle(name)(handler(_))

  protected final def jHandleObserveCommand(name: String)(handler: Observe => CompletionStage[Void]): Unit =
    handle(name)(handler(_))

  protected final def jHandleGoOnline(handler: Supplier[CompletionStage[Void]]): Unit  = onlineHandlers.add(_ => handler.get())
  protected final def jHandleAbort(handler: Supplier[CompletionStage[Void]]): Unit     = abortHandlers.add(_ => handler.get())
  protected final def jHandleShutdown(handler: Supplier[CompletionStage[Void]]): Unit  = shutdownHandlers.add(_ => handler.get())
  protected final def jHandleGoOffline(handler: Supplier[CompletionStage[Void]]): Unit = offlineHandlers.add(_ => handler.get())
}
