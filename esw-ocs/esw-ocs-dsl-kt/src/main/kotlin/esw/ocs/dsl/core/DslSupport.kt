package esw.ocs.dsl.core

import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.MainScriptDsl
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.script.exceptions.ScriptLoadingException.ScriptInitialisationFailedException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

class Result(val scriptFactory: (CswServices) -> MainScriptDsl) {
    operator fun invoke(cswService: CswServices): MainScriptDsl = scriptFactory(cswService)
}

fun script(block: suspend MainScript.(csw: CswServices) -> Unit): Result =
        Result {
            MainScript(it).apply {
                try {
                    runBlocking { block(it) }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.scriptDsl
        }


fun FSMScript(block: suspend FSMScript.(csw: CswServices) -> Unit): Result =
        Result {
            FSMScript(it).apply {
                try {
                    runBlocking { block(it) }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.scriptDsl
        }

class ReusableScriptResult(val scriptFactory: (CswServices, StrandEc, CoroutineScope) -> ReusableScript) {
    operator fun invoke(cswService: CswServices, strandEc: StrandEc, coroutineScope: CoroutineScope) =
            scriptFactory(cswService, strandEc, coroutineScope)
}

fun reusableScript(block: ReusableScript.(csw: CswServices) -> Unit) =
        ReusableScriptResult { csw, ec, ctx ->
            ReusableScript(csw, ec, ctx).apply { block(csw) }
        }
