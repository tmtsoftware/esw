package esw.ocs.dsl.highlevel

import csw.params.commands.*
import csw.params.core.models.ObsId
import esw.ocs.dsl.nullable
import java.util.*

interface CommandServiceDsl {

    fun Setup(sourcePrefix: String, commandName: String, obsId: String? = null): Setup =
            Setup(Prefix(sourcePrefix), CommandName(commandName), obsId.toOptionalObsId())

    fun Observe(sourcePrefix: String, commandName: String, obsId: String? = null): Observe =
            Observe(Prefix(sourcePrefix), CommandName(commandName), obsId.toOptionalObsId())

    fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())

    /** ========== Extensions ============ **/
    val Command.obsId: String? get() = jMaybeObsId().map { it.obsId() }.nullable()

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}
