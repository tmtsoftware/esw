package esw.ocs.dsl.highlevel

import csw.params.commands.*
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import esw.ocs.dsl.nullable
import java.util.*

/**
 * Kotlin Dsl for creating instance of command and sequence command.
 */
interface CommandServiceDsl {

    /**
     * Method to create an instance of [[csw.params.commands.Setup]]
     *
     * @param sourcePrefix Prefix string used to create Prefix representing source of the command
     * @param commandName representing the name as an identifier of a command
     * @param obsId an optional parameter represents a unique observation id
     * @return an instance of Setup command
     */
    fun Setup(sourcePrefix: String, commandName: String, obsId: String? = null): Setup =
            Setup(Prefix(sourcePrefix), CommandName(commandName), obsId.toOptionalObsId())

    /**
     * Method to create an instance of [[csw.params.commands.Observe]]
     *
     * @param sourcePrefix Prefix string used to create Prefix representing source of the command
     * @param commandName representing the name as an identifier of a command
     * @param obsId an optional parameter represents a unique observation id
     * @return an instance of Observe command
     */
    fun Observe(sourcePrefix: String, commandName: String, obsId: String? = null): Observe =
            Observe(Prefix(sourcePrefix), CommandName(commandName), obsId.toOptionalObsId())

    /**
     * Method to create an instance of [[csw.params.commands.Sequence]]
     *
     * @param sequenceCommand list of sequence commands to create sequence
     * @return an instance of Sequence
     */
    fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())

    /** ========== Extensions ============ **/

    /**
     * Extension method to get optional obsId from command
     */
    val Command.obsId: String? get() = jMaybeObsId().map { it.obsId() }.nullable()

    private fun String?.toOptionalObsId() = Optional.ofNullable(this?.let { ObsId(it) })
}
