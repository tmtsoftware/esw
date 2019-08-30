package esw.ocs.dsl

import csw.params.commands.Setup
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import kotlin.coroutines.coroutineContext

abstract class ScriptDslKt : ScriptDsl {

    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        jHandleSetupCommand(name) { setup ->

            GlobalScope.future {
                block(setup)
                null
            }
        }
    }


}