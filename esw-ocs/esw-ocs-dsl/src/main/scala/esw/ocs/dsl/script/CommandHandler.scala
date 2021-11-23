package esw.ocs.dsl.script

import csw.params.commands.SequenceCommand

import java.util.concurrent.CompletionStage

/**
 * Implementation of this trait is present at kotlin side
 * We want ability to add error handlers and retries on onSetup/onObserve command handlers
 * Having implementation at kotlin side, allows us run onError or retry handlers inside same parent coroutine
 * Hence if exception gets thrown even after all the retries, this gets propagated to top level exception handler
 */
private[script] trait CommandHandler[T <: SequenceCommand] {
  def execute(sequenceCommand: T): CompletionStage[Void]
}
