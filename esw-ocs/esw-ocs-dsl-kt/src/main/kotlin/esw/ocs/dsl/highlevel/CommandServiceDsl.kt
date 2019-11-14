package esw.ocs.dsl.highlevel

import csw.location.api.javadsl.JComponentType.Assembly
import csw.location.api.javadsl.JComponentType.HCD
import csw.location.models.ComponentType
import csw.params.commands.*
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import esw.ocs.dsl.nullable
import java.util.*

interface CommandServiceDsl {
    fun richComponent(name: String, componentType: ComponentType): RichComponent
    fun richSequencer(sequencerId: String, observingMode: String): RichSequencer

    fun setup(prefix: String, commandName: String, obsId: String? = null) =
            Setup(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun observe(prefix: String, commandName: String, obsId: String? = null) =
            Observe(Prefix(prefix), CommandName(commandName), obsId.toOptionalObsId())

    fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())

    fun Assembly(name: String): RichComponent = richComponent(name, Assembly())
    fun HCD(name: String): RichComponent = richComponent(name, HCD())
    fun Sequencer(sequencerId: String, observingMode: String): RichSequencer = richSequencer(sequencerId, observingMode)

    /** ========== Extensions ============ **/
    val Command.obsId: String? get() = jMaybeObsId().map { it.obsId() }.nullable()

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}
