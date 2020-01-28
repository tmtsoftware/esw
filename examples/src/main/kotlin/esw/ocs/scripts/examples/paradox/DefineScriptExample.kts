package esw.ocs.scripts.examples.paradox

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key
import csw.prefix.models.Prefix
//#script
import esw.ocs.dsl.core.script
//#script
import esw.ocs.dsl.highlevel.models.NFIRAOS
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.stringKey
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
    info("DarkNight script loaded")

    val tromboneTemperatureAlarm =
            Key.AlarmKey(Prefix(NFIRAOS, "trombone"), "tromboneMotorTemperatureAlarm")

    loopAsync(1.seconds) {
        setSeverity(tromboneTemperatureAlarm, getSeverity())
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
        publishEvent(ObserveEvent("IRIS.primary_shutter", "current-status", openingStatusKey))

        openPrimaryShutter()
    }

}
//#script-example