package esw.ocs.dsl.core

import esw.dsl.script.CswServices
import esw.dsl.script.ScriptDsl
import esw.ocs.macros.StrandEc

internal object ScriptDslFactory {
    fun make(cswServices: CswServices, strandEc: StrandEc) = object : ScriptDsl(cswServices) {
        override fun strandEc(): StrandEc = strandEc
    }
}
