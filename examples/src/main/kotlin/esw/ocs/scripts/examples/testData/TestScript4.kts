package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.stringKey
import kotlinx.coroutines.delay
import kotlin.time.seconds

script {

    onNewSequence {
        val newSequenceHandlerParam = stringKey("onNewSequence").set("Started")
        val event = SystemEvent("LGSF.darknight", "NewSequenceHandler", newSequenceHandlerParam)
        publishEvent(event)
        delay(500)
    }

    onSetup("command-1") {
        val newSequenceParam = stringKey("sequence-command-1").set("Started")
        val event = SystemEvent("LGSF.darknight", "command1", newSequenceParam)
        publishEvent(event)
    }

    onSetup("command-lgsf") {
        // NOT update command response To avoid sequencer to
        // finish so that other commands gets time
        delay(1000)
    }

    onAbortSequence {
        //do some actions to abort sequence
        val successEvent = SystemEvent("TCS.test", "abort.success")
        publishEvent(successEvent)
    }

    onStop {
        //do some actions to stop
        val successEvent = SystemEvent("TCS.test", "stop.success")
        publishEvent(successEvent)
    }

    onSetup("time-service-dsl") {
        val offset = utcTimeAfter(2.seconds).offsetFromNow()

        schedulePeriodically(utcTimeAfter(5.seconds), offset) {
            publishEvent(SystemEvent("LGSF.test", "publish.success"))
        }

        scheduleOnce(taiTimeNow()) {
            publishEvent(SystemEvent("LGSF.test", "publish.success"))
        }
    }
}
