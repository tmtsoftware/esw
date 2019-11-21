package esw.ocs.scripts.examples.dsl

import esw.ocs.dsl.core.script
import kotlin.time.seconds

// ESW-122 TimeServiceDsl usage in script
script {

    //Usage inside handlers - schedule tasks while handling setup/observe commands
    onSetup("schedule-periodically") {
        val offset = utcTimeAfter(2.seconds).offsetFromNow()

        schedulePeriodically(utcTimeAfter(5.seconds), offset) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }

    onObserve("schedule-once") {
        scheduleOnce(taiTimeNow()) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }

    //Usage at top level
    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }

    schedulePeriodically(utcTimeAfter(2.seconds), 5.seconds) {
        publishEvent(SystemEvent("lgsf", "publish.success"))
    }
}
