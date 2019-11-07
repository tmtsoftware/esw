package esw.ocs.scripts.examples.dsl

import esw.ocs.dsl.core.script
import kotlin.time.seconds

// ESW-122 TimeServiceDsl usage in script
script {

    //Usage inside handlers - schedule tasks while handling setup/observe commands
    handleSetup("schedule-periodically") {
        val offset = utcTimeAfter(2.seconds).offsetFromNow()

        schedulePeriodically(utcTimeAfter(5.seconds), offset) {
            publishEvent(SystemEvent("irms", "publish.success"))
        }
    }

    handleObserve("schedule-once") {
        scheduleOnce(taiTimeNow()) {
            publishEvent(SystemEvent("irms", "publish.success"))
        }
    }

    //Usage at top level
    scheduleOnce(taiTimeNow()) {
        publishEvent(SystemEvent("irms", "publish.success"))
    }

    schedulePeriodically(utcTimeAfter(2.seconds), 5.seconds) {
        publishEvent(SystemEvent("irms", "publish.success"))
    }
}
