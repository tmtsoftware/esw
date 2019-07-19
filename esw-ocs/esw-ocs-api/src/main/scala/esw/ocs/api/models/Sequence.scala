package esw.ocs.api.models

import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.serializer.OcsFrameworkAkkaSerializable

final case class Sequence private[models] (runId: Id, commands: Seq[SequenceCommand]) extends OcsFrameworkAkkaSerializable {
  def add(others: SequenceCommand*): Sequence = copy(commands = commands ++ others)
  def add(other: Sequence): Sequence          = copy(commands = commands ++ other.commands)
}

object Sequence {
  def apply(command: SequenceCommand, commands: SequenceCommand*): Sequence = Sequence(Id(), command :: commands.toList)
}
