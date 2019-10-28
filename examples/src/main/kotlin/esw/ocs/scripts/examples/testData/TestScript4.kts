package esw.ocs.scripts.examples.testData

import csw.time.core.models.TAITime
import esw.ocs.dsl.core.script
import kotlin.time.seconds

script {

    handleSetup("command-irms") { _ ->
        // NOT update command response To avoid sequencer to
        // finish so that other commands gets time
    }

    handleAbortSequence {
        //do some actions to abort sequence
        val successEvent = systemEvent("tcs", "abort.success")
        publishEvent(successEvent)
    }

    handleStop {
        //do some actions to stop
        val successEvent = systemEvent("tcs", "stop.success")
        publishEvent(successEvent)
    }

    handleSetup("time-service-dsl") { command ->
        val offset = utcTimeAfter(2.seconds).offsetFromNow()
        val taskToSchedule: suspend () -> Unit =
                { publishEvent(systemEvent("irms", "publish.success")) }

        schedulePeriodically(utcTimeAfter(5.seconds), offset, taskToSchedule)

        scheduleOnce(taiTimeNow(), taskToSchedule)
    }
}
