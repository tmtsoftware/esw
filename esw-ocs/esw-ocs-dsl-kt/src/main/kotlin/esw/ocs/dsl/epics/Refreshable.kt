/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.epics

interface Refreshable {
    suspend fun refresh()
    fun addFsmSubscription(fsmSubscription: FsmSubscription)
}
