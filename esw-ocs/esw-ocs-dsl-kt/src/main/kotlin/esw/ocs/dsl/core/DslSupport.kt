package esw.ocs.dsl.core

import esw.ocs.dsl.internal.CswServices
import esw.ocs.dsl.internal.ScriptWiring
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.exceptions.ScriptInitialisationFailedException
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.runBlocking

fun script(block: suspend ScriptScope.(csw: CswServices) -> Unit): ScriptResult =
        ScriptResult {
            val wiring = ScriptWiring(it)
            Script(wiring).apply {
                try {
                    runBlocking { block(wiring.cswServices) }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.scriptDsl
        }

fun reusableScript(block: Script.(csw: CswServices) -> Unit): ReusableScriptResult =
        ReusableScriptResult { Script(it).apply { block(it.cswServices) } }

fun FsmScript(initState: String, block: suspend FsmScriptScope.(csw: CswServices) -> Unit): ScriptResult =
        ScriptResult {
            val wiring = ScriptWiring(it)
            FsmScript(wiring).apply {
                try {
                    runBlocking {
                        block(cswServices)
                        become(initState)
                    }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.fsmScriptDsl
        }

class ScriptResult(val scriptFactory: (ScriptContext) -> ScriptDsl) {
    operator fun invoke(scriptContext: ScriptContext): ScriptDsl = scriptFactory(scriptContext)
}

class ReusableScriptResult(val scriptFactory: (ScriptWiring) -> Script) {
    operator fun invoke(wiring: ScriptWiring) =
            scriptFactory(wiring)
}
