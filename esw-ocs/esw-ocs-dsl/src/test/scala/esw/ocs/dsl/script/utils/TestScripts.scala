package esw.ocs.dsl.script.utils

import esw.ocs.dsl.script.{CswServices, JScriptDsl, StrandEc}

class ValidTestScript(csw: CswServices) extends JScriptDsl(csw) {
  override protected implicit def strandEc: StrandEc = ???
}

class InvalidTestScript(csw: CswServices)
