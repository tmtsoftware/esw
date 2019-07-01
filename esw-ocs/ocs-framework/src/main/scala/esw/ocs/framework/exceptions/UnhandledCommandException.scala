package esw.ocs.framework.exceptions

import csw.params.commands.SequenceCommand

class UnhandledCommandException(command: SequenceCommand)
    extends RuntimeException(s"command ${command.commandName} was not handled")
