/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script.utils

import scala.collection.mutable

/**
 * A builder class for a set of common functions.
 * which holds a mutable buffer of functions
 *
 * @tparam I - Type of the input param of the Function
 * @tparam O - Type of the output result of the Function
 */
private[esw] class FunctionHandlers[I, O] {
  private val handlers: mutable.Buffer[I => O] = mutable.Buffer.empty

  def add(handler: I => O): Unit = handlers += handler

  def execute(input: I): List[O] = handlers.map(f => f(input)).toList

  def ++(that: FunctionHandlers[I, O]): FunctionHandlers[I, O] = {
    this.handlers ++= that.handlers
    this
  }
}
