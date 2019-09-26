package esw.ocs.dsl.utils

import akka.Done
import csw.event.api.javadsl.IEventSubscription
import csw.params.commands.Command
import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import csw.params.core.models.Choice
import csw.params.core.models.Id
import csw.params.core.models.Units
import csw.params.javadsl.JUnits.NoUnits
import esw.ocs.dsl.internal.nullable
import kotlinx.coroutines.future.await

interface CswExtensions {
    fun GChoiceKey.set(vararg choices: Choice, units: Units = NoUnits): Parameter<Choice> = set(choices, units)

    // =========== Command ==============
    val Command.obsId: String? get() = jMaybeObsId().map { it.obsId() }.nullable()
    val Command.runId: Id get() = runId()

    // =========== Parameter ==============
    // throws exception
    operator fun <A> Parameter<A>.invoke(index: Int): A = jGet(index).get()

    val <A> Parameter<A>.values: List<A> get() = jValues().toList()

    // =========== ParameterSetType ==============
    // throws exception
    operator fun <A : ParameterSetType<A>, B> ParameterSetType<A>.invoke(keyHolder: KeyHolder<B>): Parameter<B> =
        apply(keyHolder.key)

    // fixme: merge with above
    operator fun <A : ParameterSetType<A>> ParameterSetType<A>.invoke(choice: GChoiceKey): Parameter<Choice> =
        jGet(choice).get()

    fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kAdd(vararg values: B): A =
        jMadd(values.toSet())

    fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kFind(parameter: Parameter<B>): Parameter<B>? =
        jFind(parameter).nullable()

    fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kGet(key: KeyHolder<B>): Parameter<B>? =
        jGet(key.key).nullable()

    fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kGet(
        keyName: String,
        keyType: KeyType<B>
    ): Parameter<B>? =
        jGet(keyName, keyType).nullable()

    // =========== Misc ==============
    suspend fun IEventSubscription.cancel(): Done = unsubscribe().await()

    suspend fun IEventSubscription.completed(): Done = ready().await()
}
