package esw.ocs.dsl

import csw.params.commands.Setup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future

open class ScriptDemo(csw: CswServices) : Script(csw) {
    private fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        jHandleSetupCommand(name) { setup ->

            GlobalScope.future {
                block(setup)
                null
            }
        }
    }

    init {
        handleSetup("command-1") { command ->
            println("Handler called with cmd: $command")
        }

        handleSetup("command-2") { command ->
            println("Handler called with cmd: $command")
        }
    }

}