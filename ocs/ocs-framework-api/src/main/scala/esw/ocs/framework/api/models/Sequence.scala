package esw.ocs.framework.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id

case class Sequence(runId: Id, commands: Seq[SequenceCommand]) {
  def add(others: SequenceCommand*): Sequence = copy(commands = commands ++ others)
  def add(other: Sequence): Sequence          = copy(commands = commands ++ other.commands)
}

object Sequence {
  def apply(commands: SequenceCommand*): Sequence = Sequence(Id(), commands.toList)
  def empty: Sequence                             = Sequence(Id(), Nil)
}
