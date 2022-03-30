/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

script {

    val eventVar = EventVariable("TCS.polling.event-var-test", 400.milliseconds)
    val eventVarFsm = Fsm("event-pollingTest", "INIT") {
        state("INIT") {
            val event = SystemEvent("TCS.polling", "event-var-test")
            publishEvent(event)
        }
    }

    eventVar.bind(eventVarFsm)

    onSetup("start-event-fsm") {
        eventVarFsm.start()
    }

}