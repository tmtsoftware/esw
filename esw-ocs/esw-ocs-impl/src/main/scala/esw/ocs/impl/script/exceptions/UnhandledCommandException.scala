package esw.ocs.impl.script.exceptions

import csw.params.commands.SequenceCommand

class UnhandledCommandException(command: SequenceCommand)
    extends RuntimeException(s"command ${command.commandName.name} was not handled")
