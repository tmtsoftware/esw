package esw.ocs.dsl.params

import csw.params.core.generics.Key
import csw.params.core.generics.KeyType
import csw.params.core.generics.Parameter
import csw.params.core.models.Units
import csw.params.javadsl.JUnits

// this is present just to hide java methods on Keys which are not user friendly
data class KeyKt<T>(val key: Key<T>) {
    val keyName: String = key.keyName()
    val keyType: KeyType<T> = key.keyType()
    val units: Units = key.units()
    fun set(vararg elm: T, units: Units = JUnits.NoUnits): Parameter<T> = key.set(elm, units)
}
