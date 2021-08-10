package esw.ocs.dsl.script.utils

import scala.collection.mutable

// fixme : rename FunctionBuilder
/**
 * A builder class for a set of common functions.
 * which holds a map where functions are kept as values against an unique key
 * (being used in ScriptDsl to hold the command handlers)
 *
 * @tparam K - Type of the key in the mutable map against which the function to be kept as value
 * @tparam I - Type of the input param of the Function
 * @tparam O - Type of the output result of the Function
 */
private[esw] class FunctionBuilder[K, I, O] {

  private val handlers: mutable.Map[K, I => O] = mutable.Map.empty

  def add(key: K, handler: I => O): Unit = handlers += ((key, handler))

  def contains(key: K): Boolean = handlers.contains(key)

  def execute(key: K)(input: I): O = handlers(key)(input)

  def ++(that: FunctionBuilder[K, I, O]): FunctionBuilder[K, I, O] = {
    this.handlers ++= that.handlers
    this
  }
}
