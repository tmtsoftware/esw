package esw.ocs.dsl.core

import esw.ocs.impl.dsl.CswServices
import esw.ocs.impl.dsl.javadsl.JScript
import esw.ocs.macros.StrandEc

object JScriptFactory {
    fun make(cswServices: CswServices, strandEc: StrandEc) = object : JScript(cswServices) {
        override fun strandEc(): StrandEc = strandEc
    }
}
