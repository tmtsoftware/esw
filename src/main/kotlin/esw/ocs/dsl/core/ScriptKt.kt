package esw.ocs.dsl.core

import akka.Done
import csw.event.api.scaladsl.EventSubscription
import csw.params.commands.Setup
import csw.params.events.Event
import esw.ocs.dsl.CswServices
import esw.ocs.dsl.javadsl.JScript
import esw.ocs.macros.StrandEc
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext

open class ScriptKt(val cswServices: CswServices) : CoroutineScope, JScript(cswServices) {

    private val job = Job()
    private val ec = Executors.newSingleThreadScheduledExecutor()
    private val dispatcher = ec.asCoroutineDispatcher()

    override fun strandEc(): StrandEc = StrandEc(ec)

    fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")

    fun close() {
        job.cancel()
        dispatcher.close()
    }

    override val coroutineContext: CoroutineContext
        get() = job + dispatcher


    fun handleSetup(name: String, block: suspend (Setup) -> Unit) {
        jHandleSetupCommand(name) { setup: Setup ->
            future {
                block(setup)
            }.thenAccept { }
        }
    }

    fun handleShutdown(block: suspend () -> Unit) {
        jHandleShutdown {
            future {
                block()
            }.thenAccept { }
        }
    }

    fun <T> CoroutineScope.par(block: suspend CoroutineScope.() -> T): Deferred<T> = async { block() }

    suspend fun getEvent(vararg eventKeys: String): Set<Event> = cswServices.jGetEvent(eventKeys.toSet()).await()
    suspend fun publishEvent(event: Event): Done = cswServices.jPublishEvent(event).await()
    fun onEvent(vararg eventKeys: String, callback: suspend (Event) -> Unit): EventSubscription =
        cswServices.jOnEvent(eventKeys.toSet()) {
            future {
                callback(it)
            }.thenAccept { }
        }

}