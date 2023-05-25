package esw.ocs.dsl2.highlevel

import csw.params.commands as c
import csw.params.commands.*
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix

class CommandServiceDsl {
  def Setup(sourcePrefix: String, commandName: String, obsId: String = null): Setup =
    c.Setup(Prefix(sourcePrefix), CommandName(commandName), Option(obsId).map(ObsId.apply))

  def Observe(sourcePrefix: String, commandName: String, obsId: String = null): Observe =
    c.Observe(Prefix(sourcePrefix), CommandName(commandName), Option(obsId).map(ObsId.apply))

  export c.Sequence
}
