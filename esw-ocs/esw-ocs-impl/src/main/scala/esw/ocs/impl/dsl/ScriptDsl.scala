package esw.ocs.impl.dsl

import akka.Done
import csw.params.commands.{Observe, SequenceCommand, Setup}
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.PullNextResult
import esw.ocs.impl.dsl.utils.{FunctionBuilder, FunctionHandlers}
import esw.ocs.impl.exceptions.UnhandledCommandException

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.reflect.ClassTag

class Script(val csw: CswServices) extends ScriptDsl {
  // todo: should this come from conf file?
  override private[ocs] val loopInterval = 50.millis
}

trait ScriptDsl extends ControlDsl {
  def csw: CswServices

  var isOnline = true

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, Future[Unit]] = new FunctionBuilder

  private val onlineHandlers: FunctionHandlers[Unit, Future[Unit]]                      = new FunctionHandlers
  private val offlineHandlers: FunctionHandlers[Unit, Future[Unit]]                     = new FunctionHandlers
  private val shutdownHandlers: FunctionHandlers[Unit, Future[Unit]]                    = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, Future[Unit]]                       = new FunctionHandlers
  private val diagnosticModeHandlers: FunctionHandlers[(UTCTime, String), Future[Unit]] = new FunctionHandlers
  private val operationsModeHandlers: FunctionHandlers[Unit, Future[Unit]]              = new FunctionHandlers

  private def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => Future[Unit]): Unit =
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)

  private lazy val commandHandler: SequenceCommand => Future[Unit] =
    commandHandlerBuilder.build { input =>
      // should script writer have ability to add this default handler, like handleUnknownCommand
      spawn {
        throw new UnhandledCommandException(input)
      }
    }

  private[ocs] def execute(command: SequenceCommand): Future[Unit] = spawn(commandHandler(command).await)

  private[ocs] def executeGoOnline(): Future[Done] =
    Future.sequence(onlineHandlers.execute(())).map { _ =>
      isOnline = true
      Done
    }

  private[ocs] def executeGoOffline(): Future[Done] = {
    isOnline = false
    Future.sequence(offlineHandlers.execute(())).map(_ => Done)
  }

  private[ocs] def executeShutdown(): Future[Done] = Future.sequence(shutdownHandlers.execute(())).map(_ => Done)

  private[ocs] def executeAbort(): Future[Done] = Future.sequence(abortHandlers.execute(())).map(_ => Done)

  private[ocs] def executeDiagnosticMode(startTime: UTCTime, hint: String): Future[Done] =
    Future.sequence(diagnosticModeHandlers.execute(startTime, hint)).map(_ => Done)

  private[ocs] def executeOperationsMode(): Future[Done] = Future.sequence(operationsModeHandlers.execute(())).map(_ => Done)

  protected final def nextIf(f: SequenceCommand => Boolean): Future[Option[SequenceCommand]] =
    spawn {
      val operator  = csw.sequenceOperatorFactory()
      val mayBeNext = operator.maybeNext.await
      mayBeNext match {
        case Some(step) if f(step.command) =>
          operator.pullNext.await match {
            case PullNextResult(step) => Some(step.command)
            case _                    => None
          }
        case _ => None
      }
    }

  protected final def handleSetupCommand(name: String)(handler: Setup => Future[Unit]): Unit     = handle(name)(handler)
  protected final def handleObserveCommand(name: String)(handler: Observe => Future[Unit]): Unit = handle(name)(handler)
  protected final def handleGoOnline(handler: => Future[Unit]): Unit                             = onlineHandlers.add(_ => handler)
  protected final def handleGoOffline(handler: => Future[Unit]): Unit                            = offlineHandlers.add(_ => handler)
  protected final def handleShutdown(handler: => Future[Unit]): Unit                             = shutdownHandlers.add(_ => handler)
  protected final def handleAbort(handler: => Future[Unit]): Unit                                = abortHandlers.add(_ => handler)
  protected final def handleDiagnosticMode(handler: ((UTCTime, String)) => Future[Unit]): Unit =
    diagnosticModeHandlers.add(handler)
  protected final def handleOperationsMode(handler: => Future[Unit]): Unit = operationsModeHandlers.add(_ => handler)
}
