package esw.ocs.dsl.core

import esw.ocs.dsl.CswServices
import esw.ocs.dsl.javadsl.JScript
import esw.ocs.macros.StrandEc

object JScriptFactory {
    fun make(cswServices: CswServices, strandEc: StrandEc) = object : JScript(cswServices) {
        override fun strandEc(): StrandEc = strandEc
    }
}