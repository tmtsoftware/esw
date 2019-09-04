package esw.ocs.dsl.core

import esw.ocs.dsl.CswServices
import esw.ocs.macros.StrandEc
import kotlin.coroutines.CoroutineContext

class Result(val scriptFactory: (CswServices) -> ScriptKt) {
    operator fun invoke(cswService: CswServices): ScriptKt = scriptFactory(cswService)
}

fun script(block: ScriptKt.(csw: CswServices) -> Unit): Result = Result {
    val scriptKt = ScriptKt(it)
    scriptKt.block(it)
    scriptKt
}

class ReusableScriptResult(val scriptFactory: (CswServices, StrandEc, CoroutineContext) -> ReusableScript) {
    operator fun invoke(cswService: CswServices, strandEc: StrandEc, coroutineContext: CoroutineContext) =
        scriptFactory(cswService, strandEc, coroutineContext)
}

fun reusableScript(block: ReusableScript.(csw: CswServices) -> Unit) = ReusableScriptResult { csw, ec, ctx ->
    val reusableScript = ReusableScript(csw, ec, ctx)
    reusableScript.block(csw)
    reusableScript
}
