/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.booleanKey
import esw.ocs.dsl.params.intKey
import kotlin.time.Duration.Companion.milliseconds

script {

    val onlineFlagKey = booleanKey("online-flag")
    var goOnlineCount = 0
    var goOfflineCount = 0

    loopAsync(100.milliseconds) {
        publishEvent(SystemEvent("TCS.filter.wheel", "online-flag", onlineFlagKey.set(isOnline)))
    }

    onGoOffline {
        goOfflineCount += 1
        publishEvent(SystemEvent("TCS.filter.wheel", "go-offline-handler", intKey("offline-key").set(goOfflineCount)))
    }

    onGoOnline {
        goOnlineCount += 1
        publishEvent(SystemEvent("TCS.filter.wheel", "go-online-handler", intKey("online-key").set(goOnlineCount)))
    }
}
