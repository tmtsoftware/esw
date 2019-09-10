package esw.ocs.dsl.highlevel

import akka.util.Timeout
import csw.command.client.CommandResponseManager
import csw.params.commands.CommandResponse.QueryResponse
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.impl.dsl.CswServices
import kotlinx.coroutines.future.await
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.toJavaDuration

interface CrmKtDsl {
    val cswServices: CswServices

    val crm: CommandResponseManager
        get() = cswServices.crm()

    fun addOrUpdateCommand(cmdStatus: SubmitResponse) = crm.addOrUpdateCommand(cmdStatus)

    fun addSubCommand(parentCmd: SequenceCommand, childCmd: SequenceCommand) =
        crm.addSubCommand(parentCmd.runId(), childCmd.runId())

    fun updateSubCommand(cmdStatus: SubmitResponse) = crm.updateSubCommand(cmdStatus)

    @ExperimentalTime
    suspend fun query(runId: Id, timeout: Duration): QueryResponse =
        crm.jQuery(runId, Timeout.create(timeout.toJavaDuration())).await()

    @ExperimentalTime
    suspend fun queryFinal(runId: Id, timeout: Duration): SubmitResponse =
        crm.jQueryFinal(runId, Timeout.create(timeout.toJavaDuration())).await()
}