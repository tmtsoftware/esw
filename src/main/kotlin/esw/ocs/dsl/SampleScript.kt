package esw.ocs.dsl

import csw.params.commands.CommandResponse
import csw.params.core.models.Prefix
import csw.params.events.EventName
import kotlinx.coroutines.delay
import csw.params.events.*

open class SampleScript(cswServices: CswServices) : ScriptKt(cswServices) {
    init {
        val eventKey = "csw.event.1"
        val event = SystemEvent(Prefix("csw.a.b"), EventName(eventKey))

        handleSetup("command-1") { command ->
            log("Handler called with cmd: $command")
            delay(1000)
            log("Publishing event")
            publishEvent(event)
            log("Event Published")
            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }

        handleSetup("command-2") { command ->
            log("Handler called with cmd: $command")
            log("Getting event")

            val events = getEvent(eventKey)
            log(events.toString())
            events.forEach(::println)
            cswServices.crm().addOrUpdateCommand(CommandResponse.Completed(command.runId()))
        }
    }

}