package esw.highlevel.dsl

import csw.params.commands.CommandName
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import kotlinx.coroutines.CoroutineScope
import java.util.*

interface CommandServiceKtDsl : CoroutineScope {

    fun setup(prefix: String, commandName: String, obsId: String?) =
        Setup(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun observe(prefix: String, commandName: String, obsId: String?) =
        Observe(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}