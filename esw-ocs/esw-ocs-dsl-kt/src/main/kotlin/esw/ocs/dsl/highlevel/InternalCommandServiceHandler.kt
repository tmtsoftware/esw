package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.command.api.javadsl.ICommandService
import csw.params.commands.ControlCommand
import kotlinx.coroutines.future.await

class InternalCommandServiceHandler(private val commandService: ICommandService, private val timeout: Timeout) {

    suspend fun validate(command: ControlCommand) = commandService.validate(command).await()
    suspend fun oneway(command: ControlCommand) = commandService.oneway(command, timeout).await()
    suspend fun submit(command: ControlCommand) = commandService.submit(command, timeout).await()
    suspend fun submitAndWait(command: ControlCommand) = commandService.submitAndWait(command, timeout).await()
}
