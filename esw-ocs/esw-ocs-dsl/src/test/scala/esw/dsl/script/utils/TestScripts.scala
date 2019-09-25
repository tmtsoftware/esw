package esw.dsl.script.utils

import esw.dsl.script.{CswServices, ScriptDsl}
import esw.ocs.macros.StrandEc

class ValidTestScript(csw: CswServices) extends ScriptDsl(csw) {
  override protected implicit def strandEc: StrandEc = ???
}

class InvalidTestScript(csw: CswServices)
