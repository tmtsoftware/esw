/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.utcTimeKey
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.hours

// ESW-122 TimeServiceDsl usage in script
script {

    // #schedule-once
    val scheduleTimeKey = utcTimeKey("scheduledTime")
    val schedulePrefix = "esw.test"
    val galilAssembly = Assembly(TCS, "galil")

    //Usage inside handlers - schedule tasks while handling setup/observe commands
    onSetup("schedule-once") { command ->
        val scheduledTime = command(scheduleTimeKey)
        val probeCommand = Setup(schedulePrefix, "scheduledOffset", command.obsId)

        scheduleOnce(scheduledTime.head()) {
            galilAssembly.submit(probeCommand)
        }
    }
    // #schedule-once


    // #schedule-periodically
    val offsetTimeKey = utcTimeKey("offsetTime")
    val offsetPrefix = "esw.offset"
    val assemblyForOffset = Assembly(TCS, "galil")

    onSetup("schedule-periodically") { command ->
        val probeCommand = Setup(schedulePrefix, "scheduledOffset", command.obsId)

        schedulePeriodically(interval = 5.seconds) {
            assemblyForOffset.submit(probeCommand)
        }
    }

    onSetup("schedule-periodically-with-start-time") { command ->
        val scheduledTime = command(offsetTimeKey)
        val probeCommand = Setup(schedulePrefix, "scheduledOffset", command.obsId)

        // *** schedule with start time ***
        schedulePeriodically(scheduledTime.head(), interval = 5.seconds) {
            assemblyForOffset.submit(probeCommand)
        }
    }
    // #schedule-periodically


    onSetup("schedule-once-from-now") {
        scheduleOnceFromNow(delayFromNow = 5.seconds) {
            publishEvent(SystemEvent("LGSF", "publish.success"))
        }
    }

    onSetup("schedule-periodically-from-now") {
        schedulePeriodicallyFromNow(delayFromNow = 5.seconds, interval = 1.seconds) {
            publishEvent(SystemEvent("LGSF", "publish.success"))
        }
    }

    onSetup("util-methods") {
        // #utc-time-now
        val currentUtcTime = utcTimeNow()
        // #utc-time-now
        // #tai-time-now
        val currentTaiTime = taiTimeNow()
        // #tai-time-now

        // #utc-time-after
        val utcTime = utcTimeAfter(1.hours)
        // #utc-time-after

        // #tai-time-after
        val taiTime = taiTimeAfter(1.hours)
        // #tai-time-after

        schedulePeriodicallyFromNow(delayFromNow = 5.seconds, interval = 1.seconds) {
            publishEvent(SystemEvent("LGSF", "publish.success"))
        }
    }

    //Usage at top level
    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }

    // #schedule-once-from-now
    scheduleOnceFromNow(1.hours) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }
    // #schedule-once-from-now

    schedulePeriodically(utcTimeNow(), 5.seconds) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }

    // #schedule-periodically-from-now
    schedulePeriodicallyFromNow(1.hours, 10.seconds) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }
    // #schedule-periodically-from-now
}
