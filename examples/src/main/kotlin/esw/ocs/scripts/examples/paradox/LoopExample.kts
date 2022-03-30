/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.longKey
import kotlin.time.Duration.Companion.milliseconds

script {
    //#loopAsync-default-interval
    var stopPublishingTemperature = false
    val temperatureEvent = SystemEvent("IRIS.motor", "temperature")
    val temperatureKey = longKey("temperature")

    fun getCurrentTemp(): Long = TODO()

    //#loopAsync-default-interval

    //#loop-default-interval
    var motorPosition = 0
    //#loop-default-interval

    //#waitFor
    var motorUp = false
    //#waitFor

    //#loopAsync-default-interval
    // start background loop which publishes current temperature of motor every 50 milliseconds (default loop interval)
    loopAsync {
        val currentTemp = getCurrentTemp()
        publishEvent(temperatureEvent.add(temperatureKey.set(currentTemp)))
        stopWhen(stopPublishingTemperature)
    }
    //#loopAsync-default-interval

    //#loopAsync-custom-interval
    // start background loop which publishes current temperature of motor every 100 milliseconds
    loopAsync(minInterval = 100.milliseconds) {
        val currentTemp = getCurrentTemp()
        publishEvent(temperatureEvent.add(temperatureKey.set(currentTemp)))
        stopWhen(stopPublishingTemperature)
    }
    //#loopAsync-custom-interval


    //#waitFor

    fun initializeMotor() {
        // some motor initialization logic goes here
        motorUp = true
    }
    onSetup("init-motor") {
        // start initializing motor and this method will set motorUp flag to true once initialization is successful
        initializeMotor()
        // pauses the init-motor command handlers execution until motor becomes up
        waitFor { motorUp }

        // rest of the handler implementation (here you can safely assume that motor is up)
    }
    //#waitFor

    //#loop-default-interval
    fun moveMotor(degrees: Int) {
        // move motor logic
        motorPosition += degrees
    }
    //#loop-custom-interval
    onSetup("move-motor") {

        val expectedMotorPosition = 100
        //#loop-custom-interval

        // move motor by 10 degrees in each iteration, default loop interval is 50 millis
        // stop loop when current motor position matches expected motor position and continue with the execution of rest of the handler
        loop {
            moveMotor(10)
            stopWhen(motorPosition == expectedMotorPosition)
        }
        //#loop-default-interval

        //#loop-custom-interval
        // move motor by 20 degrees in every iteration after a loop interval of 500 millis (custom loop interval used here)
        // stop loop when current motor position matches expected motor position and continue with the execution of rest of the handler
        loop(minInterval = 500.milliseconds) {
            moveMotor(20)
            stopWhen(motorPosition == expectedMotorPosition)
        }
    // #loop-default-interval
    }
    //#loop-default-interval
    //#loop-custom-interval

    //#loopAsync-default-interval

    onStop {
        stopPublishingTemperature = true
    }
    //#loopAsync-default-interval

}
