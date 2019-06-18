package esw.ocs.framework.dsl.internal

import scala.collection.mutable
import scala.util.Try

private[framework] class FunctionHandlers[I, O] {
  private val handlers: mutable.Buffer[I ⇒ O] = mutable.Buffer.empty

  def add(handler: I ⇒ O): Unit = handlers += handler

  def execute(input: I): mutable.Buffer[Try[O]] = handlers.map(f ⇒ Try(f(input)))
}
