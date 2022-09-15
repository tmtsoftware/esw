/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel.models

data class EventSubscription(private val unsubscribe: suspend () -> Unit) {
    suspend fun cancel() = unsubscribe() // to solve mocking issue, we had to introduce this function: https://github.com/mockk/mockk/issues/288
}