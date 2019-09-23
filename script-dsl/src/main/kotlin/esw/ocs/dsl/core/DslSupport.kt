package esw.ocs.dsl.core

import esw.dsl.script.CswServices
import esw.ocs.macros.StrandEc
import kotlin.coroutines.CoroutineContext

class Result(val scriptFactory: (CswServices) -> ScriptKt) {
    operator fun invoke(cswService: CswServices): ScriptKt = scriptFactory(cswService)
}

fun script(block: ScriptKt.(csw: CswServices) -> Unit): Result =
    Result {
        ScriptKt(it).apply { block(it) }
    }

class ReusableScriptResult(val scriptFactory: (CswServices, StrandEc, CoroutineContext) -> ReusableScript) {
    operator fun invoke(cswService: CswServices, strandEc: StrandEc, coroutineContext: CoroutineContext) =
        scriptFactory(cswService, strandEc, coroutineContext)
}

fun reusableScript(block: ReusableScript.(csw: CswServices) -> Unit) =
    ReusableScriptResult { csw, ec, ctx ->
        ReusableScript(csw, ec, ctx).apply { block(csw) }
    }
