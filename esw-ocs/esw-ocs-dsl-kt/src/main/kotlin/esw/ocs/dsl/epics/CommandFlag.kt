/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.epics

import esw.ocs.dsl.params.Params

class CommandFlag {
    private var params: Params = Params(setOf())
    private val subscribers: MutableSet<Refreshable> = mutableSetOf()

    fun bind(refreshable: Refreshable) {
        subscribers.add(refreshable)
    }

    suspend fun set(_params: Params) {
        params = _params
        subscribers.forEach {
            it.refresh()
        }
    }

    fun value() = params
}
