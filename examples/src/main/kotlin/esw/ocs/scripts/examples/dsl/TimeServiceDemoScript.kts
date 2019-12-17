package esw.ocs.scripts.examples.dsl

import esw.ocs.dsl.core.script
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

    //Usage at top level
    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }

    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }

    schedulePeriodically(utcTimeNow(), 5.seconds) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }

    schedulePeriodicallyFromNow(5.seconds, 1.seconds) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }
}
