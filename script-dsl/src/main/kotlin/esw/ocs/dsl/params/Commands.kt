package esw.ocs.dsl.params

import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType

fun <P : Parameter<*>, T : ParameterSetType<T>> ParameterSetType<T>.madd(vararg values: P): T = jMadd(values.toSet())
