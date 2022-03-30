/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData.sharedState

import esw.ocs.dsl.core.reusableScript
import kotlinx.coroutines.launch

val counterIncrementer = reusableScript {
    // keep incrementing counter 100_000 times in the background while processing commands
    val job = coroutineScope.launch {
        repeat(100_000) { counter++ }
    }

    onSetup("increment") {
        repeat(100_000) { counter++ }
        job.join()
    }
}