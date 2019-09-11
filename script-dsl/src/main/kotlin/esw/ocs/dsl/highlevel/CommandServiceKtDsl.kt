package esw.ocs.dsl.highlevel

import csw.params.commands.CommandName
import csw.params.commands.CommandResponse.*
import csw.params.commands.ControlCommand
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import esw.ocs.impl.dsl.CswServices
import kotlinx.coroutines.future.await
import java.util.*

interface CommandServiceKtDsl {

    val cswServices: CswServices

    fun setup(prefix: String, commandName: String, obsId: String?) =
        Setup(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun observe(prefix: String, commandName: String, obsId: String?) =
        Observe(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    /************* Assembly *************/
    suspend fun validateAssemblyCommand(assemblyName: String, command: ControlCommand): ValidateResponse =
        cswServices.validateAssemblyCommand(assemblyName, command).await()

    suspend fun submitCommandToAssembly(assemblyName: String, command: ControlCommand): SubmitResponse =
        cswServices.submitCommandToAssembly(assemblyName, command).await()

    suspend fun submitAndWaitCommandToAssembly(assemblyName: String, command: ControlCommand): SubmitResponse =
        cswServices.submitAndWaitCommandToAssembly(assemblyName, command).await()

    suspend fun oneWayCommandToAssembly(assemblyName: String, command: ControlCommand): OnewayResponse =
        cswServices.oneWayCommandToAssembly(assemblyName, command).await()

    /************* HCD *************/
    suspend fun validateHcdCommand(assemblyName: String, command: ControlCommand): ValidateResponse =
        cswServices.validateHcdCommand(assemblyName, command).await()

    suspend fun submitCommandToHcd(hcdName: String, command: ControlCommand): SubmitResponse =
        cswServices.submitCommandToHcd(hcdName, command).await()

    suspend fun submitAndWaitCommandToHcd(hcdName: String, command: ControlCommand): SubmitResponse =
        cswServices.submitAndWaitCommandToHcd(hcdName, command).await()

    suspend fun oneWayCommandToHcd(hcdName: String, command: ControlCommand): OnewayResponse =
        cswServices.oneWayCommandToHcd(hcdName, command).await()

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}