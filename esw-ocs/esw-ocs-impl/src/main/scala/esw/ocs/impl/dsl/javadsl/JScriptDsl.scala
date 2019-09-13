package esw.ocs.impl.dsl.javadsl

import java.time.Duration
import java.util.Optional
import java.util.concurrent.{CompletableFuture, CompletionStage, TimeUnit}
import java.util.function.Supplier

import akka.Done
import csw.params.commands.{Observe, SequenceCommand, Setup}
import esw.ocs.api.protocol._
import esw.ocs.impl.dsl.utils.{FunctionBuilder, FunctionHandlers}
import esw.ocs.impl.dsl.{BaseScriptDsl, CswServices}
import esw.ocs.impl.exceptions.UnhandledCommandException

import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}
import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.reflect.ClassTag

abstract class JScript(override val csw: CswServices) extends JScriptDsl {
  override private[esw] val loopInterval: FiniteDuration = 50.millis
}

// fixme : should Control dsl written for java
trait JScriptDsl extends BaseScriptDsl {
  def csw: CswServices

  var isOnline = true

  private val commandHandlerBuilder: FunctionBuilder[SequenceCommand, CompletionStage[Void]] = new FunctionBuilder

  private val onlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]   = new FunctionHandlers
  private val offlineHandlers: FunctionHandlers[Unit, CompletionStage[Void]]  = new FunctionHandlers
  private val shutdownHandlers: FunctionHandlers[Unit, CompletionStage[Void]] = new FunctionHandlers
  private val abortHandlers: FunctionHandlers[Unit, CompletionStage[Void]]    = new FunctionHandlers

  private[esw] def merge(that: JScriptDsl): JScriptDsl = {

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
      // should script writer have ability to add this default handler, like handleUnknownCommand
      CompletableFuture.failedFuture(new UnhandledCommandException(input))
    }

  // fixme : Try removing scala-java conversions (toScala)
  private[ocs] def execute(command: SequenceCommand): Future[Unit] = spawn(commandHandler(command).toScala.await)

  private[ocs] def executeGoOnline(): Future[Done] =
    // fixme: remove map(_.toScala)  from private APIs
    Future.sequence(onlineHandlers.execute(()).map(_.toScala)).map { _ =>
      isOnline = true
      Done
    }

  private[ocs] def executeGoOffline(): Future[Done] = {
    isOnline = false
    Future.sequence(offlineHandlers.execute(()).map(_.toScala)).map(_ => Done)
  }

  private[ocs] def executeShutdown(): Future[Done] = Future.sequence(shutdownHandlers.execute(()).map(_.toScala)).map(_ => Done)

  private[ocs] def executeAbort(): Future[Done] = Future.sequence(abortHandlers.execute(()).map(_.toScala)).map(_ => Done)

  protected final def jNextIf(f: SequenceCommand => Boolean): CompletionStage[Optional[SequenceCommand]] = {
    spawn {
      val operator  = csw.sequenceOperatorFactory()
      val mayBeNext = operator.maybeNext.await
      mayBeNext match {
        case Some(step) if f(step.command) =>
          operator.pullNext.await match {
            case PullNextResult(step) => Optional.ofNullable(step.command)
            case _                    => Optional.empty[SequenceCommand]
          }
        case _ => Optional.empty[SequenceCommand]
      }
    }.toJava
  }

  protected final def jHandleSetupCommand(name: String)(handler: Setup => CompletionStage[Void]): Void = {
    handle(name)(handler(_))
    null
  }

  protected final def jHandleObserveCommand(name: String)(handler: Observe => CompletionStage[Void]): Void = {
    handle(name)(handler(_))
    null
  }

  protected final def jHandleGoOnline(handler: Supplier[CompletionStage[Void]]): Void = {
    onlineHandlers.add(_ => handler.get())
    null
  }

  protected final def jHandleAbort(handler: Supplier[CompletionStage[Void]]): Void = {
    abortHandlers.add(_ => handler.get())
    null
  }

  protected final def jHandleShutdown(handler: Supplier[CompletionStage[Void]]): Void = {
    shutdownHandlers.add(_ => handler.get())
    null
  }

  protected final def jHandleGoOffline(handler: Supplier[CompletionStage[Void]]): Void = {
    offlineHandlers.add(_ => handler.get())
    null
  }

  protected def jLoop(duration: Duration, block: Supplier[CompletionStage[StopIf]]): CompletionStage[Void] = {
    loop(FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS))(block.get().toScala).toJava.thenApply(_ => null)
  }
}
