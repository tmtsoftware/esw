package esw.ocs.dsl.params

import csw.params.core.generics.Parameter
import esw.ocs.dsl.utils.nullable

data class ParameterKt<T>(val parameter: Parameter<T>) {
    val values: List<T> = parameter.jValues().toList()

    fun get(index: Int): T? = parameter.jGet(index).nullable()
    fun first(): T = parameter.head()

    operator fun invoke(index: Int): T = parameter.value(index)
}
