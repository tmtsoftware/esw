package esw.ocs.dsl.core

import esw.ocs.dsl.script.CswServices
import esw.ocs.dsl.script.JScriptDsl
import esw.ocs.dsl.script.StrandEc

internal object ScriptDslFactory {
    fun make(cswServices: CswServices, strandEc: StrandEc) = object : JScriptDsl(cswServices) {
        override fun strandEc(): StrandEc = strandEc
    }
}
