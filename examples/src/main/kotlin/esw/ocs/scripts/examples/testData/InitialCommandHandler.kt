/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.reusableScript
import kotlinx.coroutines.delay

// ESW-134: Reuse code by ability to import logic from one script into another
val InitialCommandHandler = reusableScript {
    onSetup("command-1") {
        // To avoid a sequencer to finish immediately so that other commands like Add, Append get time
        delay(200)
    }
}