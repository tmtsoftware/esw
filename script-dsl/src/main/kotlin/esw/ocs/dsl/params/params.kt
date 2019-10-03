package esw.ocs.dsl.params

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import esw.ocs.dsl.nullable

/** ========== Parameter =========== **/
val <T> Parameter<T>.values: List<T> get() = jValues().toList()
val <T> Parameter<T>.first: T get() = head()

operator fun <T> Parameter<T>.get(index: Int): T? = jGet(index).nullable()
operator fun <T> Parameter<T>.invoke(index: Int): T = value(index)

/** ========== ParameterSetType =========== **/
fun <T : ParameterSetType<T>, P : Parameter<*>> T.madd(vararg parameters: P): T = jMadd(parameters.toSet())

fun <T : ParameterSetType<T>, S> T.find(parameter: Parameter<S>): Parameter<S>? = jFind(parameter).nullable()
fun <T : ParameterSetType<T>, S> T.exists(key: Key<S>): Boolean = exists(key)

fun <T : ParameterSetType<T>, S> T.remove(key: Key<S>): T = remove(key)
fun <T : ParameterSetType<T>, P : Parameter<*>> T.remove(parameter: P): T = remove(parameter)

operator fun <T : ParameterSetType<T>, S> T.get(key: Key<S>): Parameter<S>? = jGet(key).nullable()
operator fun <T : ParameterSetType<T>, S> T.get(keyName: String, keyType: KeyType<S>): Parameter<S>? =
    jGet(keyName, keyType).nullable()

operator fun <T : ParameterSetType<T>, S> T.invoke(key: Key<S>): Parameter<S> = apply(key)
