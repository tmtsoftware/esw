/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript

val exceptionHandlerScript = reusableScript {
    onGlobalError { exception ->
        val successEvent = SystemEvent("TCS.filter.wheel", exception.reason)
        publishEvent(successEvent)
    }
}