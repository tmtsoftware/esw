@file:Suppress("UNUSED_VARIABLE", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_PARAMETER", "ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key
import csw.params.events.SystemEvent
import csw.prefix.models.Prefix
//#fsm-script
import esw.ocs.dsl.core.FsmScript
//#fsm-script
//#reusable-script-example
import esw.ocs.dsl.core.reusableScript
//#reusable-script-example
//#script
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.Major
//#script
import esw.ocs.dsl.highlevel.models.NFIRAOS
import esw.ocs.dsl.highlevel.models.Okay
import esw.ocs.dsl.params.*
import kotlin.time.seconds

fun moveMotor(angle: Int): Unit = TODO()
fun openPrimaryShutter(): Unit = TODO()
fun getSeverity(): AlarmSeverity = TODO()

//#script

script {
    // place to add Sequencer Script logic
}
//#script

//#script-example
script {
    info("Loading DarkNight script")

    var tromboneTemperature = 0.0
    onEvent("nfiraos.tromboneAssembly.temperature") { event ->
        when (event) {
            is SystemEvent -> {
                tromboneTemperature = event(doubleKey("temperatureInCelsius")).first
            }
        }
    }

    val tromboneTemperatureAlarm =
            Key.AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneMotorTemperatureAlarm")

    loopAsync(1.seconds) {
        if (tromboneTemperature > 10.0) {
            setSeverity(tromboneTemperatureAlarm, Major)
        } else  {
            setSeverity(tromboneTemperatureAlarm, Okay)
        }
    }

    onSetup("basic-setup") { command ->

        val intKey = intKey("angle")
        val angle = command.parameter(intKey).head()!!

        info("moving motor by : $angle")
        moveMotor(angle)
        info("motor moved to required position")
    }

    onObserve("start-observation") {
        info("opening the primary shutter to start observation")

        val openingStatusKey = stringKey("status").set("open")
        publishEvent(SystemEvent("IRIS.primary_shutter", "current-status", openingStatusKey))

        openPrimaryShutter()
    }

}
//#script-example


//#fsm-script

FsmScript("INIT") {

    // Default scope
    // place for Script variable declarations and initialisation statements

    state("INIT") { params ->
        // Scope of INIT state
        // handlers of INTI state
    }

    state("IN-PROGRESS") {
        // Scope of IN-PROGRESS state
        // handlers of IN-PROGRESS state
    }

}
//#fsm-script

fun turnOnLight(): Unit = TODO()
fun turnOffLight(): Unit = TODO()


FsmScript("OFF") {

    //#fsm-script-become
    state("ON") { params ->

        onSetup("turn-off") {
            turnOffLight()
            become("OFF")                           // [[ 1 ]]
        }
    }

    state("OFF") {

        onSetup("turn-on") { command ->
            turnOnLight()
            become("ON", command.params)           // [[ 2 ]]
        }
    }
    //#fsm-script-become

    fun moveBy(angle: Int): Unit = TODO()
    fun stopSetup(): Unit = TODO()

    //#fsm-script-state
    state("SETTING-UP") { params ->

        val initialPos = params[intKey("current-position")].get().head()
        var moved = false

        onSetup("move") { command ->
            val angle = command.params[intKey("angle")].get().head()
            moveBy(angle)
            moved = true

            info("moved from : $initialPos by angle : $angle")

            become("READY")
        }

        onGoOffline {
            stopSetup()
            info("Going in offline mode")
        }

    }
    //#fsm-script-state

}


//#reusable-script-example

val startObservationScript = reusableScript {
    onObserve("start-observation") {
        info("opening the primary shutter to start observation")

        val openingStatusKey = stringKey("status").set("open")
        publishEvent(SystemEvent("IRIS.primary_shutter", "current-status", openingStatusKey))

        openPrimaryShutter()
    }

}
//#reusable-script-example

//#load-script
script {

    loadScripts(startObservationScript)

}
//#load-script


FsmScript("INIT") {

    //#load-script-fsm
    state("INIT") { params ->

        loadScripts(startObservationScript)

    }
    //#load-script-fsm
}
