package esw.ocs.scripts.examples.paradox

import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.longKey
import kotlin.time.milliseconds

script {
    var stopPublishingTemperature = false
    val temperatureEvent = ObserveEvent("iris.motor", "temperature")
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

    // start background loop which publishes current temperature of motor every 50 milliseconds (default loop interval)
    bgLoop {
        val currentTemp = getCurrentTemp()
        publishEvent(temperatureEvent.add(temperatureKey.set(currentTemp)))
        stopWhen(stopPublishingTemperature)
    }

    // start background loop which publishes current temperature of motor every 100 milliseconds
    bgLoop(100.milliseconds) {
        val currentTemp = getCurrentTemp()
        publishEvent(temperatureEvent.add(temperatureKey.set(currentTemp)))
        stopWhen(stopPublishingTemperature)
    }

    onSetup("init-motor") {
        initializeMotor()
        // pauses the init-motor command handlers execution until motor becomes up
        waitFor { motorUp }

        // rest of the handler implementation (here you can safely assume that motor is up)
    }

    onSetup("move-motor") {
        val expectedMotorPosition = 100

        // move motor by 10 degrees in every iteration after a loop interval of 50 millis which is the default value
        // stop loop when current motor position matches expected motor position and continue with the execution of rest of the handler
        loop {
            moveMotor(10)
            stopWhen(motorPosition == expectedMotorPosition)
        }

        // move motor by 20 degrees in every iteration after a loop interval of 100 millis (custom loop interval used here)
        // stop loop when current motor position matches expected motor position and continue with the execution of rest of the handler
        loop(100.milliseconds) {
            moveMotor(20)
            stopWhen(motorPosition == expectedMotorPosition)
        }
    }

    onStop {
        stopPublishingTemperature = true
    }

}