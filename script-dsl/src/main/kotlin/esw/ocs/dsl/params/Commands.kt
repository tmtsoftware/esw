package esw.ocs.dsl.params

import csw.params.commands.Command
import csw.params.core.generics.GChoiceKey
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import csw.params.core.models.Choice
import esw.ocs.dsl.nullable

fun <A : ParameterSetType<A>, B : Parameter<*>> ParameterSetType<A>.madd(vararg values: B): A = jMadd(values.toSet())

val Command.obsId: String?
    get() {
        return jMaybeObsId().map { it.obsId() }.nullable()
    }

// throws exception
operator fun <A : ParameterSetType<A>, B> ParameterSetType<A>.invoke(keyHolder: KeyHolder<B>): Parameter<B> {
    return jGet(keyHolder.key).get()
}

// fixme: merge with above
operator fun <A : ParameterSetType<A>> ParameterSetType<A>.invoke(choice: GChoiceKey): Parameter<Choice> {
    return jGet(choice).get()
}

// throws exception
operator fun <A> Parameter<A>.invoke(index: Int): A {
    return jGet(index).get()
}
