package esw.ocs.dsl.params

import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import csw.params.core.models.Choice

fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.madd(vararg values: B): A = jMadd(values.toSet())

// throws exception
operator fun <A : ParameterSetType<A>, B> ParameterSetType<A>.invoke(keyHolder: KeyHolder<B>): Parameter<B> {
    return jGet(keyHolder.key).get()
}

// fixme: merge with above
operator fun <A : ParameterSetType<A>> ParameterSetType<A>.invoke(choice: GChoiceKey): Parameter<Choice> {
    return jGet(choice).get()
}
