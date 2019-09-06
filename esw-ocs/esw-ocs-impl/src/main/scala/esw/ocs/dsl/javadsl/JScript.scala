package esw.ocs.dsl.javadsl

import java.time
import java.util.Optional
import java.util.concurrent.{CompletionStage, TimeUnit}
import java.util.function.Supplier

import csw.params.commands.{Observe, SequenceCommand, Setup}
import esw.ocs.dsl.{CswServices, ScriptDsl, StopIf}

import scala.compat.java8.FutureConverters.{CompletionStageOps, FutureOps}
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.jdk.OptionConverters.RichOption
import scala.language.implicitConversions

abstract class JScript(override val csw: CswServices) extends ScriptDsl {
  override private[esw] val loopInterval: FiniteDuration = 50.millis

  protected def jNextIf(f: SequenceCommand => Boolean): CompletionStage[Optional[SequenceCommand]] = {
    nextIf(f).map(_.toJava).toJava
  }

  protected def jHandleSetupCommand(name: String, handler: Setup => CompletionStage[Void]): Void = {
    handleSetupCommand(name)(handler(_))
    null
  }

  protected def jHandleObserveCommand(name: String, handler: Observe => CompletionStage[Void]): Void = {
    handleObserveCommand(name)(handler(_))
    null
  }

  protected def jHandleGoOnline(handler: Supplier[CompletionStage[Void]]): Void = {
    handleGoOnline(handler.get())
    null
  }
  protected def jHandleGoOffline(handler: Supplier[CompletionStage[Void]]): Void = {
    handleGoOffline(handler.get())
    null
  }
  protected def jHandleShutdown(handler: Supplier[CompletionStage[Void]]): Void = {
    handleShutdown(handler.get())
    null
  }

  protected def jHandleAbort(handler: Supplier[CompletionStage[Void]]): Void = {
    handleAbort(handler.get())
    null
  }

  protected def jLoop(duration: time.Duration, block: Supplier[CompletionStage[StopIf]]): CompletionStage[Void] = {
    loop(FiniteDuration(duration.toNanos, TimeUnit.NANOSECONDS))(block.get().toScala).toJava.thenApply(_ => null)
  }

  implicit private def mapToScala(f: CompletionStage[Void]): Future[Unit] = f.toScala.map(_ => ())
}
