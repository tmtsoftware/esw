@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.TCS
import esw.ocs.dsl.params.invoke
import esw.ocs.dsl.params.utcTimeKey
import kotlin.time.Duration

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

        schedulePeriodically(interval = Duration.seconds(5)) {
            assemblyForOffset.submit(probeCommand)
        }
    }

    onSetup("schedule-periodically-with-start-time") { command ->
        val scheduledTime = command(offsetTimeKey)
        val probeCommand = Setup(schedulePrefix, "scheduledOffset", command.obsId)

        // *** schedule with start time ***
        schedulePeriodically(scheduledTime.head(), interval = Duration.seconds(5)) {
            assemblyForOffset.submit(probeCommand)
        }
    }
    // #schedule-periodically


    onSetup("schedule-once-from-now") {
        scheduleOnceFromNow(delayFromNow = Duration.seconds(5)) {
            publishEvent(SystemEvent("LGSF", "publish.success"))
        }
    }

    onSetup("schedule-periodically-from-now") {
        schedulePeriodicallyFromNow(delayFromNow = Duration.seconds(5), interval = Duration.seconds(1)) {
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
        val utcTime = utcTimeAfter(Duration.hours(1))
        // #utc-time-after

        // #tai-time-after
        val taiTime = taiTimeAfter(Duration.hours(1))
        // #tai-time-after

        schedulePeriodicallyFromNow(delayFromNow = Duration.seconds(5), interval = Duration.seconds(1)) {
            publishEvent(SystemEvent("LGSF", "publish.success"))
        }
    }

    //Usage at top level
    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }

    // #schedule-once-from-now
    scheduleOnceFromNow(Duration.hours(1)) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }
    // #schedule-once-from-now

    schedulePeriodically(utcTimeNow(), Duration.seconds(5)) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }

    // #schedule-periodically-from-now
    schedulePeriodicallyFromNow(Duration.hours(1), Duration.seconds(10)) {
        publishEvent(SystemEvent("LGSF", "publish.success"))
    }
    // #schedule-periodically-from-now
}
