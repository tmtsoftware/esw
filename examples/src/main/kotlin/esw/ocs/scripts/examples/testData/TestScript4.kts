package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay
import kotlin.time.seconds

script {

    handleSetup("command-irms") {
        // NOT update command response To avoid sequencer to
        // finish so that other commands gets time
        delay(10000)
    }

    handleAbortSequence {
        //do some actions to abort sequence
        val successEvent = SystemEvent("tcs", "abort.success")
        publishEvent(successEvent)
    }

    handleStop {
        //do some actions to stop
        val successEvent = SystemEvent("tcs", "stop.success")
        publishEvent(successEvent)
    }

    handleSetup("time-service-dsl") {
        val offset = utcTimeAfter(2.seconds).offsetFromNow()

        schedulePeriodically(utcTimeAfter(5.seconds), offset) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }

        scheduleOnce(taiTimeNow()) {
            publishEvent(SystemEvent("lgsf", "publish.success"))
        }
    }
}
