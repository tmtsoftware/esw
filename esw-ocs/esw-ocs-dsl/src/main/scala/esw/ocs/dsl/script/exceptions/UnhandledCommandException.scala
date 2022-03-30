/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script.exceptions

import csw.params.commands.SequenceCommand

class UnhandledCommandException(command: SequenceCommand)
    extends RuntimeException(s"command ${command.commandName.name} was not handled")
