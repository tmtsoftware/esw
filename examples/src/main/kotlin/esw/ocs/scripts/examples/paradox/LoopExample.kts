@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.longKey
import kotlin.time.milliseconds

script {
    var stopPublishingTemperature = false
    val temperatureEvent = ObserveEvent("IRIS.motor", "temperature")
    val temperatureKey = longKey("temperature")
    var motorUp = false
    var motorPosition = 0

    fun getCurrentTemp(): Long = TODO()

    fun initializeMotor() {
        // motor initialization logic
        motorUp = true
    }

    fun moveMotor(degrees: Int) {
        // move motor logic
        motorPosition += degrees
    }

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

    onSetup("init-motor") {
        //#waitFor
        // start initializing motor and this method will set motorUp flag to true once initialization is successful
        initializeMotor()
        // pauses the init-motor command handlers execution until motor becomes up
        waitFor { motorUp }
        //#waitFor

        // rest of the handler implementation (here you can safely assume that motor is up)
    }

    onSetup("move-motor") {

        //#loop-default-interval
        val expectedMotorPosition = 100

        // move motor by 10 degrees in each iteration, default loop interval is 50 millis
        // stop loop when current motor position matches expected motor position and continue with the execution of rest of the handler
        loop {
            moveMotor(10)
            stopWhen(motorPosition == expectedMotorPosition)
        }
        //#loop-default-interval

        //#loop-custom-interval
        // move motor by 20 degrees in every iteration after a loop interval of 100 millis (custom loop interval used here)
        // stop loop when current motor position matches expected motor position and continue with the execution of rest of the handler
        loop(minInterval = 100.milliseconds) {
            moveMotor(20)
            stopWhen(motorPosition == expectedMotorPosition)
        }
        //#loop-custom-interval
    }

    onStop {
        stopPublishingTemperature = true
    }

}
