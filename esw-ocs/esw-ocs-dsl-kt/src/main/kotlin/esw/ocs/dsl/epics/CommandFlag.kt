package esw.ocs.dsl.epics

import csw.params.core.generics.Parameter
import esw.ocs.dsl.params.Params

class CommandFlag(){
    private var params: Params = Params(setOf())
    private val subscribers: Set<Refreshable> = mutableSetOf()

    fun bind(refreshable: Refreshable) {
        subscribers + refreshable
    }

    fun set(paramsSet: Set<Parameter<*>>) {
        params = Params(paramsSet)
        subscribers.forEach { it.refresh() }
    }

    fun value() = params
}
