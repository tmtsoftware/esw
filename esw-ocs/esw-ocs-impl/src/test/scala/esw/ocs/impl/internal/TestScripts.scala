package esw.ocs.impl.internal

import esw.dsl.script.CswServices
import esw.dsl.script.javadsl.JScript
import esw.ocs.macros.StrandEc

class ValidTestScript(csw: CswServices) extends JScript(csw) {
  override protected implicit def strandEc: StrandEc = ???
}
class InvalidTestScript(csw: CswServices)
