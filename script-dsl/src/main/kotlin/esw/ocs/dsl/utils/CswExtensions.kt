package esw.ocs.dsl.utils

import akka.Done
import csw.event.api.javadsl.IEventSubscription
import csw.params.commands.Command
import csw.params.commands.Sequence
import csw.params.commands.SequenceCommand
import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import csw.params.core.models.Choice
import csw.params.core.models.Id
import csw.params.core.models.Prefix
import csw.params.core.models.Units
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.javadsl.JUnits.NoUnits
import esw.ocs.dsl.params.KeyKt
import esw.ocs.dsl.params.ParameterKt
import esw.ocs.dsl.params.ParameterSetTypeKt
import kotlinx.coroutines.future.await

interface CswExtensions {
    fun GChoiceKey.set(vararg choices: Choice, units: Units = NoUnits): Parameter<Choice> = set(choices, units)

    fun eventKey(prefix: String, eventName: String) = EventKey(Prefix(prefix), EventName(eventName))
    fun sequenceOf(vararg sequenceCommand: SequenceCommand): Sequence = Sequence.create(sequenceCommand.toList())

    // =========== Command ==============
    val Command.obsId: String? get() = jMaybeObsId().map { it.obsId() }.nullable()
    val Command.runId: Id get() = runId()

    fun <T : ParameterSetType<T>> T.params(): ParameterSetTypeKt<T> = ParameterSetTypeKt(this)

    operator fun <S> Event.invoke(keyKt: KeyKt<S>): ParameterKt<S> = ParameterKt(paramType().apply(keyKt.key))
    operator fun Event.invoke(choiceKey: GChoiceKey): ParameterKt<Choice> = ParameterKt(paramType().apply(choiceKey))
    operator fun <S> Command.invoke(keyKt: KeyKt<S>): ParameterKt<S> = ParameterKt(paramType().apply(keyKt.key))
    operator fun Command.invoke(choiceKey: GChoiceKey): ParameterKt<Choice> = ParameterKt(paramType().apply(choiceKey))

    // =========== Misc ==============
    suspend fun IEventSubscription.cancel(): Done = unsubscribe().await()

    suspend fun IEventSubscription.completed(): Done = ready().await()
}
