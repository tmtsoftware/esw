package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse.QueryResponse
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.dsl.script.CswServices
import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.future.await

interface CrmKtDsl {
    val cswServices: CswServices

    val crm: CommandResponseManager
        get() = cswServices.crm()

    fun addOrUpdateCommand(cmdStatus: SubmitResponse) = crm.addOrUpdateCommand(cmdStatus)

    fun addSubCommand(parentCmd: SequenceCommand, childCmd: SequenceCommand) =
        crm.addSubCommand(parentCmd.runId(), childCmd.runId())

    fun updateSubCommand(cmdStatus: SubmitResponse) = crm.updateSubCommand(cmdStatus)

    suspend fun query(runId: Id, timeout: Duration): QueryResponse =
        crm.jQuery(runId, Timeout.create(timeout.toJavaDuration())).await()

    suspend fun queryFinal(runId: Id, timeout: Duration): SubmitResponse =
        crm.jQueryFinal(runId, Timeout.create(timeout.toJavaDuration())).await()
}
