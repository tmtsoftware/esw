package esw.ocs.dsl.core

import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptInitialisationFailedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

fun script(block: suspend Script.(csw: CswServices) -> Unit): ScriptResult =
        ScriptResult {
            val wiring = ScriptWiring()
            Script(it, wiring.strandEc, wiring.scope).apply {
                try {
                    runBlocking { block(it) }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.scriptDsl
        }

fun reusableScript(block: Script.(csw: CswServices) -> Unit): ReusableScriptResult =
        ReusableScriptResult { csw, ec, ctx ->
            Script(csw, ec, ctx).apply { block(csw) }
        }

fun FSMScript(initState: String, block: suspend FSMScript.(csw: CswServices) -> Unit): ScriptResult =
        ScriptResult {
            val wiring = ScriptWiring()
            FSMScript(it, wiring.strandEc, wiring.scope).apply {
                try {
                    runBlocking {
                        block(it)
                        become(initState)
                    }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.fsmScriptDsl
        }

class ScriptResult(val scriptFactory: (CswServices) -> ScriptDsl) {
    operator fun invoke(cswService: CswServices): ScriptDsl = scriptFactory(cswService)
}

class ReusableScriptResult(val scriptFactory: (CswServices, StrandEc, CoroutineScope) -> Script) {
    operator fun invoke(cswService: CswServices, strandEc: StrandEc, coroutineScope: CoroutineScope) =
            scriptFactory(cswService, strandEc, coroutineScope)
}
