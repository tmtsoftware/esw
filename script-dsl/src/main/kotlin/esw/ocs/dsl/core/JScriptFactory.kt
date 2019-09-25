package esw.ocs.dsl.core

import esw.dsl.script.CswServices
import esw.dsl.script.javadsl.JScript
import esw.ocs.macros.StrandEc

object JScriptFactory {
    fun make(cswServices: CswServices, strandEc: StrandEc) = object : JScript(cswServices) {
        override fun strandEc(): StrandEc = strandEc
    }
}
