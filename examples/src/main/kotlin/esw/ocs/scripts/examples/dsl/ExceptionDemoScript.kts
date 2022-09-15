/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.dsl

import csw.alarm.api.exceptions.KeyNotFoundException
import csw.event.api.exceptions.EventServerNotAvailable
import esw.ocs.dsl.core.script

script {

    // *************** Script initialisation **********

    //  throws EventServerNotAvailable exception
    //  1. returns ScriptError with the message of the exception
    //  2. script initialisation fails
    publishEvent(SystemEvent("TCS", "test.event"))


    scheduleOnce(utcTimeNow()) {
        //  throws EventServerNotAvailable exception
        //  1. which will call exception handler
        publishEvent(SystemEvent("TCS", "test.event"))
    }

    onGlobalError { error ->
        when (error.cause) {
            is EventServerNotAvailable -> {
                println(error)
            }

            is KeyNotFoundException -> {
                println(error)
            }
        }
    }

    onSetup("command-1") {
        // throws EventServerNotAvailable exception
        // 1. which will call exception handler
        // 2. mark the command as Error.
        publishEvent(SystemEvent("TCS", "test.event"))
    }


    onSetup("command-2") {
        scheduleOnce(utcTimeNow()) {
            // throws EventServerNotAvailable exception
            // 1. which will call exception handler
            publishEvent(SystemEvent("TCS", "test.event"))
        }

        // more APIs like scheduleOnce are  "schedulePeriodically, publishAsync, OnEvent"
    }
    // IMP — exceptions thrown from Exception Handlers will be logged and then ignored.

}