package esw.ocs.dsl.params

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import csw.params.core.models.Choice
import esw.ocs.dsl.nullable

// throws exception
operator fun <A : ParameterSetType<A>, B> ParameterSetType<A>.invoke(keyHolder: KeyHolder<B>): Parameter<B> =
    apply(keyHolder.key)

// fixme: merge with above
operator fun <A : ParameterSetType<A>> ParameterSetType<A>.invoke(choice: GChoiceKey): Parameter<Choice> =
    jGet(choice).get()

fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kAdd(vararg values: B): A = jMadd(values.toSet())

fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kFind(parameter: Parameter<B>): Parameter<B>? =
    jFind(parameter).nullable()

fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kGet(key: KeyHolder<B>): Parameter<B>? =
    jGet(key.key).nullable()

fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.kGet(
    keyName: String,
    keyType: KeyType<B>
): Parameter<B>? =
    jGet(keyName, keyType).nullable()
