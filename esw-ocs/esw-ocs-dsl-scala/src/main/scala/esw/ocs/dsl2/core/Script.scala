package esw.ocs.dsl2.core

import csw.logging.api.scaladsl.Logger
import csw.params.commands.{Observe, SequenceCommand, Setup}
import csw.time.core.models.UTCTime
import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.script.{ScriptDsl, StrandEc}
import esw.ocs.dsl2.Extensions
import esw.ocs.dsl2.highlevel.CswHighLevelDsl
import esw.ocs.dsl2.highlevel.models.ScriptError
import esw.ocs.dsl2.internal.ScriptWiring
import esw.ocs.impl.script.ScriptContext
import csw.prefix.models.Subsystem

import async.Async.*
import scala.compat.java8.FutureConverters.FutureOps
import scala.concurrent.ExecutionContext
import scala.jdk.FutureConverters.CompletionStageOps
import scala.util.control.NonFatal

class Basescript(scriptContext: ScriptContext):

  val scriptWiring                     = new ScriptWiring(scriptContext)
  val scriptDsl: ScriptDsl             = scriptWiring.scriptDsl
  val cswHighLevelDsl: CswHighLevelDsl = scriptWiring.cswHighLevelDsl

  export cswHighLevelDsl.{prefix as _, *}
  export Extensions.*
  export Subsystem.*

  given ExecutionContext = strandEc.ecWithReporter { exception =>
    logger.error(
      s"Exception thrown in script with the message: [${exception.getMessage}], invoking exception handler"
    )
    scriptDsl.executeExceptionHandlers(exception)
  }

  val strandEcForShutdown = strandEc.ecWithReporter { exception =>
    logger.error(s"Shutting down: Exception thrown in script with the message: [${exception.getMessage}]")
  }

  def isOnline: Boolean = scriptDsl.isOnline
  val obsMode: ObsMode  = scriptContext.obsMode

  inline def onNewSequence(inline block: Unit): Unit   = scriptDsl.onNewSequence(() => async({ block; null }).toJava)
  inline def onGoOnline(inline block: Unit): Unit      = scriptDsl.onGoOnline(() => async({ block; null }).toJava)
  inline def onGoOffline(inline block: Unit): Unit     = scriptDsl.onGoOffline(() => async({ block; null }).toJava)
  inline def onAbortSequence(inline block: Unit): Unit = scriptDsl.onAbortSequence(() => async({ block; null }).toJava)

  inline def onShutdown(inline block: Unit): Unit =
    scriptDsl
      .onShutdown(() => {
        val future = async {
          block
          scriptWiring.shutdown()
          null
        }(using strandEcForShutdown)
        future.toJava
      })

  inline def onDiagnosticMode(inline block: (UTCTime, String) => Unit) =
    scriptDsl.onDiagnosticMode((a, b) => async({ block(a, b); null }).toJava)

  inline def onOperationsMode(inline block: Unit) = scriptDsl.onOperationsMode(() => async({ block; null }).toJava)
  inline def onStop(inline block: Unit)           = scriptDsl.onStop(() => async({ block; null }).toJava)

end Basescript

class Script(scriptContext: ScriptContext) extends Basescript(scriptContext):
  inline def nextIf(predicate: SequenceCommand => Boolean): SequenceCommand = await(scriptDsl.nextIf(predicate).asScala).get()

  inline def onSetup(name: String)(inline block: Setup => Unit): CommandHandlerScala[Setup] =
    val handler = new CommandHandlerScala[Setup](cmd => async(block(cmd)), cswHighLevelDsl.loopDsl)
    scriptDsl.onSetupCommand(name)(handler)
    handler

  inline def onObserve(name: String, inline block: Observe => Unit): CommandHandlerScala[Observe] =
    val handler = new CommandHandlerScala[Observe](cmd => async(block(cmd)), cswHighLevelDsl.loopDsl)
    scriptDsl.onObserveCommand(name)(handler)
    handler

  inline def onGlobalError(inline block: ScriptError => Unit): Unit =
    scriptDsl.onException { ex =>
      // "future" is used to swallow the exception coming from exception handlers
      async({ block(ex.toScriptError); null }).recover { case NonFatal(ex) =>
        cswHighLevelDsl.logger.error(
          s"Exception thrown from Exception handler with a message : ${ex.getMessage}",
          ex = ex
        )
        null
      }.toJava
    }
end Script
