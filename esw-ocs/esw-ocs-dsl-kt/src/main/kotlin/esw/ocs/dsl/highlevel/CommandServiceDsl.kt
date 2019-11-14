package esw.ocs.dsl.highlevel

import csw.params.commands.*
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import esw.ocs.dsl.nullable
import java.util.*

interface CommandServiceDsl {

    fun setup(prefix: String, commandName: String, obsId: String? = null) =
            Setup(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun observe(prefix: String, commandName: String, obsId: String? = null) =
            Observe(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())

    /** ========== Extensions ============ **/
    val Command.obsId: String? get() = jMaybeObsId().map { it.obsId() }.nullable()

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}
