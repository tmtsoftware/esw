@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.ObserveEvent
import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.epics.EventVariable
import esw.ocs.dsl.epics.ParamVariable
import esw.ocs.dsl.params.intKey
import kotlin.time.seconds

script {

    onSetup("publish-event") {


        //#system-event
        //#observe-event
        val parameters = intKey("stepNumber").set(1)
        //#observe-event
        //#publish
        val systemEvent: SystemEvent = SystemEvent("ESW.IRIS_darkNight", "stepInfo", parameters)
        //#publish
        //#system-event

        //#observe-event
        val observeEvent: ObserveEvent = ObserveEvent("ESW.IRIS_darkNight", "observationStarted")
        //#observe-event

        //#publish
        publishEvent(systemEvent)
        //#publish

        //#subscribe
        //#get-event
        val tempEventKey = "IRIS.env.temperature.temp"
        val stateEventKey = "IRIS.env.temperature.state"
        //#get-event
        onEvent(tempEventKey, stateEventKey) { event ->
            // logic to execute on every event
            println(event.eventKey())
        }
        //#subscribe

        //#subscribe-async
        onEvent(tempEventKey, stateEventKey, duration = 2.seconds) { event ->
            // logic to execute on every event
            println(event.eventKey())
        }
        //#subscribe-async

        fun getTemperature(): Int = TODO()

        //#publish-async
        publishEvent(10.seconds) {
            val temperatureKey = intKey("temperature").set(getTemperature())
            SystemEvent("ESW.IRIS_darkNight", "temperature", temperatureKey)
        }
        //#publish-async

        //#get-event
        val events: Set<Event> = getEvent(tempEventKey, stateEventKey)
        //#get-event

        //#event-key
        // full event key string
        val tempKey: EventKey = EventKey("ESW.temperature.temp")

        // prefix and event name strings
        val tempKey1: EventKey = EventKey("ESW.temperature", "temp")
        //#event-key

        //#event-var
        val eventVariable: EventVariable = EventVariable("ESW.temperature.temp")
        //#event-var

        //#param-var
        val locKey = intKey("current-location")
        val paramVariable: ParamVariable<Int> = ParamVariable(0, "IRIS.ifs.motor.position", locKey)
        //#param-var

    }

}
