package esw.ocs.dsl.script.utils

import esw.ocs.dsl.script.{CswServices, ScriptDsl, StrandEc}

class ValidTestScript(csw: CswServices) extends ScriptDsl(csw) {
  override protected implicit def strandEc: StrandEc = ???
}

class InvalidTestScript(csw: CswServices)
