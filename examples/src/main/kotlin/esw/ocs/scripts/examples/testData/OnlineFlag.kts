package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.booleanKey
import kotlin.time.milliseconds

script {

    val onlineFlagKey = booleanKey("online-flag")

    loopAsync(100.milliseconds) {
        publishEvent(SystemEvent("TCS.filter.wheel", "online-flag", onlineFlagKey.set(isOnline)))
    }

}