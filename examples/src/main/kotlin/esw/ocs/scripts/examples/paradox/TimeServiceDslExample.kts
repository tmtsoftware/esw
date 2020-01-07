@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import kotlin.time.hours
import kotlin.time.seconds

// ESW-122 TimeServiceDsl usage in script
script {

    //Usage inside handlers - schedule tasks while handling setup/observe commands
    onSetup("schedule-once-from-now") {
        scheduleOnceFromNow(durationFromNow = 5.seconds) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }

    onObserve("schedule-once") {
        scheduleOnce(startTime = taiTimeNow()) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }

    onSetup("schedule-periodically") {
        schedulePeriodically(startTime = utcTimeNow(), interval = 1.seconds) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }

    onSetup("schedule-periodically-from-now") {
        schedulePeriodicallyFromNow(durationFromNow = 5.seconds, interval = 1.seconds) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
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

        schedulePeriodicallyFromNow(durationFromNow = 5.seconds, interval = 1.seconds) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }

    //Usage at top level
    // #schedule-once
    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }
    // #schedule-once

    // #schedule-once-from-now
    scheduleOnceFromNow(1.hours) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }
    // #schedule-once-from-now

    // #schedule-periodically
    schedulePeriodically(utcTimeNow(), 5.seconds) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }
    // #schedule-periodically

    // #schedule-periodically-from-now
    schedulePeriodicallyFromNow(1.hours, 10.seconds) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }
    // #schedule-periodically-from-now
}
