package esw.ocs.dsl

import akka.Done
import csw.params.commands.{Observe, SequenceCommand, Setup}
import esw.ocs.dsl.utils.{FunctionBuilder, FunctionHandlers}
import esw.ocs.exceptions.UnhandledCommandException

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.reflect.ClassTag

class Script(val csw: CswServices) extends ScriptDsl {
  // todo: should this come from conf file?
  override private[ocs] val loopInterval = 50.millis
}

trait ScriptDsl extends ControlDsl {
  def csw: CswServices

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, Future[Unit]] = new FunctionBuilder

  private val goOnlineHandlers: FunctionHandlers[Unit, Future[Unit]]  = new FunctionHandlers
  private val goOfflineHandlers: FunctionHandlers[Unit, Future[Unit]] = new FunctionHandlers
  private val shutdownHandlers: FunctionHandlers[Unit, Future[Unit]]  = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, Future[Unit]]     = new FunctionHandlers

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

  // this futures will normally run in parallel, but given that those are running on same thread
  // this will executes sequentially
  private[ocs] def executeGoOnline(): Future[Done]  = Future.sequence(goOnlineHandlers.execute(())).map(_ => Done)
  private[ocs] def executeGoOffline(): Future[Done] = Future.sequence(goOfflineHandlers.execute(())).map(_ => Done)
  private[ocs] def executeShutdown(): Future[Done]  = Future.sequence(shutdownHandlers.execute(())).map(_ => Done)
  private[ocs] def executeAbort(): Future[Done]     = Future.sequence(abortHandlers.execute(())).map(_ => Done)

  protected final def nextIf(f: SequenceCommand => Boolean): Future[Option[SequenceCommand]] =
    spawn {
      val operator = csw.sequenceOperatorFactory()
      operator.maybeNext.await.map(_.command) match {
        case Some(cmd) if f(cmd) => Some(operator.pullNext.await.command)
        case _                   => None
      }
    }

  protected final def handleSetupCommand(name: String)(handler: Setup => Future[Unit]): Unit     = handle(name)(handler)
  protected final def handleObserveCommand(name: String)(handler: Observe => Future[Unit]): Unit = handle(name)(handler)
  protected final def handleShutdown(handler: => Future[Unit]): Unit                             = shutdownHandlers.add(_ => handler)
  protected final def handleAbort(handler: => Future[Unit]): Unit                                = abortHandlers.add(_ => handler)
}
