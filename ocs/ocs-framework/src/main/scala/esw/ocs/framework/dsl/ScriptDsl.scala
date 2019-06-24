package esw.ocs.framework.dsl

import csw.params.commands.{Observe, SequenceCommand, Setup}
import esw.ocs.framework.dsl.internal.{FunctionBuilder, FunctionHandlers}
import esw.ocs.framework.exceptions.UnhandledCommandException

import scala.concurrent.Future
import scala.reflect.ClassTag

trait ScriptDsl extends ControlDsl {
  def csw: CswServices

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, Future[Unit]] = new FunctionBuilder

  private val shutdownHandlers: FunctionHandlers[Unit, Future[Unit]] = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, Future[Unit]]    = new FunctionHandlers

  private def handle[T <: SequenceCommand: ClassTag](name: String)(handler: T => Future[Unit]): Unit =
    commandHandlerBuilder.addHandler[T](handler)(_.commandName.name == name)

  private lazy val commandHandler: SequenceCommand => Future[Unit] =
    commandHandlerBuilder.build { input =>
      // should script writer have ability to add this default handler, like handleUnknownCommand
      spawn {
        throw new UnhandledCommandException(input)
      }
    }

  private[framework] def execute(command: SequenceCommand): Future[Unit] = spawn(commandHandler(command).await)

  // this futures will normally run in parallel, but given that those are running on same thread
  // this will executes sequentially
  private[framework] def executeShutdown(): Future[List[Unit]] = Future.sequence(shutdownHandlers.execute(()))
  private[framework] def executeAbort(): Future[List[Unit]]    = Future.sequence(abortHandlers.execute(()))

  protected final def handleSetupCommand(name: String)(handler: Setup => Future[Unit]): Unit     = handle(name)(handler)
  protected final def handleObserveCommand(name: String)(handler: Observe => Future[Unit]): Unit = handle(name)(handler)
  protected final def handleShutdown(handler: => Future[Unit]): Unit                             = shutdownHandlers.add(_ => handler)
  protected final def handleAbort(handler: => Future[Unit]): Unit                                = abortHandlers.add(_ => handler)
}
