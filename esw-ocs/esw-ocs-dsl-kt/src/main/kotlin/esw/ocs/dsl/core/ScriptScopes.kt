package esw.ocs.dsl.core

import csw.params.commands.Observe
import csw.params.commands.SequenceCommand
import csw.params.commands.Setup
import csw.time.core.models.UTCTime
import esw.ocs.dsl.ScriptDslMarker
import esw.ocs.dsl.highlevel.CswHighLevelDslApi
import esw.ocs.dsl.params.Params
import kotlinx.coroutines.CoroutineScope

interface CommonHandlers : CswHighLevelDslApi {
    fun onGoOnline(block: suspend HandlerScope.() -> Unit)
    fun onGoOffline(block: suspend HandlerScope.() -> Unit)
    fun onAbortSequence(block: suspend HandlerScope.() -> Unit)
    fun onShutdown(block: suspend HandlerScope.() -> Unit)
    fun onDiagnosticMode(block: suspend HandlerScope.(UTCTime, String) -> Unit)
    fun onOperationsMode(block: suspend HandlerScope.() -> Unit)
    fun onStop(block: suspend HandlerScope.() -> Unit)
}

interface ScriptHandlers {
    fun onSetup(name: String, block: suspend CommandHandlerScope.(Setup) -> Unit): CommandHandlerKt<Setup>
    fun onObserve(name: String, block: suspend CommandHandlerScope.(Observe) -> Unit): CommandHandlerKt<Observe>
    fun onException(block: suspend HandlerScope.(Throwable) -> Unit)
    fun loadScripts(vararg reusableScriptResult: ReusableScriptResult)
}

interface BecomeDsl {
    fun become(nextState: String, params: Params = Params(setOf()))
}

interface NextIfDsl {
    suspend fun nextIf(predicate: (SequenceCommand) -> Boolean): SequenceCommand?
}

@ScriptDslMarker
interface HandlerScope : CswHighLevelDslApi, BecomeDsl, CoroutineScope

@ScriptDslMarker
interface CommandHandlerScope : HandlerScope, NextIfDsl

//--------------------------------------- Script ----------------------------//

@ScriptDslMarker
interface ScriptScope : ScriptHandlers, CommonHandlers

//--------------------------------------- FSMScript --------------------------//

@ScriptDslMarker
interface FSMScriptStateScope : ScriptScope, BecomeDsl, CoroutineScope

@ScriptDslMarker
interface FSMScriptScope : CommonHandlers {
    fun state(name: String, block: suspend FSMScriptStateScope.(Params) -> Unit)
}