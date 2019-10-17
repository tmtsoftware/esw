package esw.ocs.dsl.script

import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage}
import java.util.function.Supplier

import akka.Done
import akka.actor.typed.ActorSystem
import csw.command.client.SequencerCommandServiceFactory
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.{Observe, Sequence, SequenceCommand, Setup}
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.PullNextResult
import esw.ocs.dsl.script.exceptions.UnhandledCommandException
import esw.ocs.dsl.script.utils.{FunctionBuilder, FunctionHandlers}
import esw.ocs.dsl.sequence_manager.LocationServiceUtil

import scala.async.Async.{async, await}
import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag

abstract class JScriptDsl(val csw: CswServices) {

  protected implicit def strandEc: StrandEc
  protected implicit lazy val toEc: ExecutionContext = strandEc.ec

  var isOnline = true

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, CompletionStage[Void]] = new FunctionBuilder

  private val onlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                  = new FunctionHandlers
  private val offlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                 = new FunctionHandlers
  private val shutdownHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, CompletionStage[Void]]                   = new FunctionHandlers
  private val diagnosticHandlers: FunctionHandlers[(UTCTime, String), CompletionStage[Void]] = new FunctionHandlers
  private val operationsHandlers: FunctionHandlers[Unit, CompletionStage[Void]]              = new FunctionHandlers
  private var initializer: Supplier[Void]                                                    = () => null // default handler

  private[esw] def merge(that: JScriptDsl): JScriptDsl = {
    commandHandlerBuilder ++ that.commandHandlerBuilder
    onlineHandlers ++ that.onlineHandlers
    offlineHandlers ++ that.offlineHandlers
    shutdownHandlers ++ that.shutdownHandlers
    abortHandlers ++ that.abortHandlers
    diagnosticHandlers ++ that.diagnosticHandlers
    operationsHandlers ++ that.operationsHandlers
    this
  }

  private def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => CompletionStage[Void]): Unit =
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)

  private lazy val commandHandler: SequenceCommand => CompletionStage[Void] =
    commandHandlerBuilder.build { input =>
      // fixme: should script writer have ability to add this default handler, like handleUnknownCommand
      CompletableFuture.failedFuture(new UnhandledCommandException(input))
    }

  private[esw] def execute(command: SequenceCommand): Future[Unit] = commandHandler(command).toScala.map(_ => ())

  private def executeHandler[T, S](f: FunctionHandlers[Unit, CompletionStage[Void]]): Future[Unit] =
    Future.sequence(f.execute(()).map(_.toScala)).map(_ => ())

  private[esw] def executeGoOnline(): Future[Done] =
    executeHandler(onlineHandlers).map { _ =>
      isOnline = true
      Done
    }

  private[esw] def executeGoOffline(): Future[Done] = {
    isOnline = false
    executeHandler(offlineHandlers).map(_ => Done)
  }

  private[esw] def executeShutdown(): Future[Done] = executeHandler(shutdownHandlers).map(_ => Done)

  private[esw] def executeAbort(): Future[Done] = executeHandler(abortHandlers).map(_ => Done)

  private[esw] def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    Future.sequence(diagnosticHandlers.execute((startTime, hint)).map(_.toScala)).map(_ => Done)

  private[esw] def executeOperationsMode(): Future[Done] = executeHandler(operationsHandlers).map(_ => Done)

  protected final def nextIf(f: SequenceCommand => Boolean): CompletionStage[Optional[SequenceCommand]] =
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

  private[esw] def initialize(): Unit = initializer.get()

  protected final def addInitializer(block: Supplier[Void]): Unit = initializer = block

  protected final def handleSetupCommand(name: String)(handler: Setup => CompletionStage[Void]): Unit =
    handle(name)(handler(_))

  protected final def handleObserveCommand(name: String)(handler: Observe => CompletionStage[Void]): Unit =
    handle(name)(handler(_))

  protected final def handleGoOnline(handler: Supplier[CompletionStage[Void]]): Unit  = onlineHandlers.add(_ => handler.get())
  protected final def handleAbort(handler: Supplier[CompletionStage[Void]]): Unit     = abortHandlers.add(_ => handler.get())
  protected final def handleShutdown(handler: Supplier[CompletionStage[Void]]): Unit  = shutdownHandlers.add(_ => handler.get())
  protected final def handleGoOffline(handler: Supplier[CompletionStage[Void]]): Unit = offlineHandlers.add(_ => handler.get())
  protected final def handleDiagnosticMode(handler: (UTCTime, String) => CompletionStage[Void]): Unit =
    diagnosticHandlers.add((x: (UTCTime, String)) => handler(x._1, x._2))
  protected final def handleOperationsMode(handler: Supplier[CompletionStage[Void]]): Unit =
    operationsHandlers.add(_ => handler.get())

  protected final def submitSequence(
      sequencerName: String,
      observingMode: String,
      sequence: Sequence
  ): CompletionStage[SubmitResponse] = {
    implicit val actorSystem: ActorSystem[_] = csw.actorSystem
    new LocationServiceUtil(csw.locationService.asScala)
      .resolveSequencer(sequencerName, observingMode)
      .map(SequencerCommandServiceFactory.make)
      .flatMap(_.submitAndWait(sequence))
      .toJava
  }
}
