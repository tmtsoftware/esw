package esw.ocs.dsl.script.utils

import scala.collection.mutable

// fixme : rename FunctionBuilder
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
