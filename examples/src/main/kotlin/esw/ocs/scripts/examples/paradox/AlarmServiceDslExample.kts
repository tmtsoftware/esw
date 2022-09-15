/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.alarm.models.Key.AlarmKey
import csw.prefix.models.Prefix
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.Major
import esw.ocs.dsl.highlevel.models.NFIRAOS
import esw.ocs.dsl.highlevel.models.Okay
import esw.ocs.dsl.params.longKey

script {
    // temperature Fsm states
    val OK = "OK"
    val ERROR = "ERROR"

    val tempKey = longKey("temperature")
    val temperatureVar = ParamVariable(0, "esw.temperature.temp", tempKey)

    //#alarm-key
    val tromboneTemperatureAlarm =
            AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneMotorTemperatureAlarm")
    //#alarm-key

    //#set-severity
    /**
     * temp <= 40   => Severity.Okay
     * else        => Severity.Major
     */
    val temperatureFsm = Fsm("TEMP", OK) {
        state(OK) {
            entry {
                setSeverity(tromboneTemperatureAlarm, Okay)
            }

            on(temperatureVar.first() > 40) {
                become(ERROR)
            }
        }

        state(ERROR) {
            entry {
                setSeverity(tromboneTemperatureAlarm, Major)
            }

            on(temperatureVar.first() <= 40) {
                become(OK)
            }
        }
    }
    //#set-severity

    temperatureVar.bind(temperatureFsm)
}