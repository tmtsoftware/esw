package esw.ocs.dsl.core

import esw.ocs.dsl.CswServices

class Result(val scriptFactory: (CswServices) -> ScriptKt)

fun script(block: ScriptKt.() -> Unit): Result = Result {
    val scriptKt = ScriptKt(it)
    block(scriptKt)
    scriptKt
}
