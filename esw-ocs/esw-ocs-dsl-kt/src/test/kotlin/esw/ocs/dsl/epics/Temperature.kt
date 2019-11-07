package esw.ocs.dsl.epics

import akka.Done
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventSubscriber
import csw.event.api.javadsl.IEventSubscription
import csw.params.events.Event
import esw.ocs.dsl.params.intKey
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CompletableFuture
import kotlin.time.seconds

typealias SubscriptionCallback = suspend (Event) -> Unit

abstract class TestMachine(name: String, init: String) : Machine(name, init) {
    private var database = mutableMapOf<String, Event>()
    private var subscriptions = mutableMapOf<String, List<SubscriptionCallback>>()

    override val defaultPublisher: IEventPublisher
        get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

    override val defaultSubscriber: IEventSubscriber
        get() = TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

    private val mockedEventSubscription = object : IEventSubscription {
        override fun unsubscribe(): CompletableFuture<Done> = TODO("not implemented")
        override fun ready(): CompletableFuture<Done> = TODO("not implemented")
    }

    override suspend fun publishEvent(event: Event): Done {
        database[event.eventKey().key()] = event
        subscriptions[event.eventKey().key()]?.forEach { it(event) }
        return Done.done()
    }

    override suspend fun onEvent(vararg eventKeys: String, callback: suspend (Event) -> Unit): IEventSubscription {
        eventKeys.map {
            subscriptions.merge(it, listOf(callback)) { old, new -> old + new }
        }
        return mockedEventSubscription
    }

    override suspend fun getEvent(vararg eventKeys: String): Set<Event> =
        eventKeys.mapNotNull { database[it] }.toSet()

    val prefix = "esw.epic"
    val tempKey = intKey("temp")
}

// ESW-228: SPIKE : snl , epics in kotlin env
val machine1 = object : TestMachine("temp-get", "Init") {

    var temp = Var(0, "$prefix.temp", tempKey)

    override suspend fun logic(state: String) {
        when (state) {
            "Init" -> {
                `when`() {
                    temp.set(45)
                    temp.pvPut()
                    become("Ok")
                }
            }

            "Ok" -> {
                `when`(temp.get() > 40) {
                    temp.set(25)
                    become("High")
                }
            }

            "High" -> {
                `when`(temp.get() < 30) {
                    //                    temp.pvGet()
                    become("Ok")
                }
            }
        }
    }

    override fun debugString(): String = "temp = $temp"
}

val machine2 = object : TestMachine("temp-monitor", "Init") {
    var temp by reactiveEvent(0, "$prefix.temp", tempKey) { _, _ ->
    }

    override suspend fun logic(state: String) {
        when (state) {
            "Init" -> {
                `when`() {
                    temp = 45
                    become("Ok")
                }
            }

            "Ok" -> {
                `when`(temp > 40) {
                    temp = 25
                    become("High")
                }
            }

            "High" -> {
                `when`(temp < 30) {
                    become("Ok")
                }
            }
        }
    }

    override fun debugString(): String = "temp = $temp"
}

val machine3 = object : TestMachine("temp-monitor", "Init") {
    var temp = Var(0, "$prefix.temp", tempKey)
    var counter = 0

    init {
        // for testing purpose
        bgLoop(1.seconds) {
            val value = (20..60).random()
            temp.set(value)
            temp.pvPut()
            counter += 1
            stopWhen(counter == 10)
        }
    }

    override suspend fun logic(state: String) {
        when (state) {
            "Init" -> {
                entry {
                    temp.pvMonitor()
                }
                `when`(1.seconds) {
                    become("Ok")
                }
            }

            "Ok" -> {
                `when`(temp.get() > 40) {
                    temp.set(25)
                    become("High")
                }
            }

            "High" -> {
                `when`(temp.get() < 30) {
                    become("Ok")
                }
            }
        }
    }

    override fun debugString(): String = "temp = $temp"
}

fun main() = runBlocking {
    println("============= MACHINE 1 =============")
    machine1.refresh("Init")

    println()
    println("============= MACHINE 2 =============")
    machine2.refresh("Init")

    println()
    println("============= MACHINE 3 =============")
    machine3.refresh("Init")
}
