package esw.ocs.dsl.core

import esw.ocs.dsl.internal.ScriptWiring
import esw.ocs.dsl.script.ScriptDsl
import esw.ocs.dsl.script.exceptions.ScriptInitialisationFailedException
import esw.ocs.impl.script.ScriptContext
import kotlinx.coroutines.runBlocking

//these methods are being used by script-writers to write different type of scripts

/**
 * Method to write basic sequencer script
 *
 * @param block - code within the script e.g., handlers etc
 *
 * @return an instance of [[esw.ocs.dsl.core.ScriptResult]]
 */
fun script(block: suspend ScriptScope.() -> Unit): ScriptResult =
        ScriptResult {
            val wiring = ScriptWiring(it)
            Script(wiring).apply {
                try {
                    runBlocking {
                        startHealthCheck()
                        block()
                    }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.scriptDsl
        }

/**
 * Method to write reusable script(common script)
 *
 * @param block - code within the script e.g., handlers etc
 *
 * @return an instance of [[esw.ocs.dsl.core.ReusableScriptResult]]
 */
fun reusableScript(block: ScriptScope.() -> Unit): ReusableScriptResult =
        ReusableScriptResult { Script(it).apply { block() } }

/**
 * Method to write FSM script
 *
 * @param block - code within the script
 *
 * @return an instance of [[esw.ocs.dsl.core.ScriptResult]]
 */
fun FsmScript(initState: String, block: suspend FsmScriptScope.() -> Unit): ScriptResult =
        ScriptResult {
            val wiring = ScriptWiring(it)
            FsmScript(wiring).apply {
                try {
                    runBlocking {
                        block()
                        become(initState)
                    }
                } catch (ex: Exception) {
                    error("Script initialisation failed with message : " + ex.message)
                    throw ScriptInitialisationFailedException(ex.message)
                }
            }.fsmScriptDsl
        }

/**
 * This class is being used to load scripts(in Scala) using Reflection.
 * Since Sequencer has been implemented in scala and scripts are being written in Kotlin
 * we have to use reflection(java reflection) to get the object of these scripts at runtime
 * and then use the script to load in the sequencer
 *
 * @property scriptFactory - a factory method which takes [esw.ocs.impl.script.ScriptContext] and returns [esw.ocs.dsl.script.ScriptDsl]
 */
class ScriptResult(val scriptFactory: (ScriptContext) -> ScriptDsl) {
    operator fun invoke(scriptContext: ScriptContext): ScriptDsl = scriptFactory(scriptContext)
}

/**
 * This class is being used to write reusable scripts which are being used later in the scripts.
 *
 * @property scriptFactory - a factory method which takes [esw.ocs.dsl.internal.ScriptWiring] and returns [esw.ocs.dsl.core.Script]
 */
class ReusableScriptResult(val scriptFactory: (ScriptWiring) -> Script) {
    operator fun invoke(wiring: ScriptWiring): Script = scriptFactory(wiring)
}
