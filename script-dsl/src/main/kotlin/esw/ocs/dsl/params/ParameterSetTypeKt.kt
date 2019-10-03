package esw.ocs.dsl.params

import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.generics.ParameterSetType
import esw.ocs.dsl.utils.nullable

data class ParameterSetTypeKt<T : ParameterSetType<T>>(val paramSetType: T) {
    val paramSet: List<Parameter<*>> = paramSetType.jParamSet().toList()
    val size: Int = paramSetType.size()

    fun <P : Parameter<*>> add(parameter: P): T = paramSetType.add(parameter)
    fun <P : Parameter<*>> madd(vararg parameters: P): T = paramSetType.jMadd(parameters.toSet())
    fun <S> get(key: KeyKt<S>): Parameter<S>? = paramSetType.jGet(key.key).nullable()
    fun <S> get(keyName: String, keyType: KeyType<S>): Parameter<S>? = paramSetType.jGet(keyName, keyType).nullable()
    fun <S> find(parameter: Parameter<S>): Parameter<S>? = paramSetType.jFind(parameter).nullable()
    fun <S> exists(key: KeyKt<S>): Boolean = paramSetType.exists(key.key)
    fun <S> remove(key: KeyKt<S>): T = paramSetType.remove(key.key)
    fun <P : Parameter<*>> remove(parameter: P): T = paramSetType.remove(parameter)
    operator fun <S> invoke(key: KeyKt<S>): Parameter<S> = paramSetType.apply(key.key)
}
