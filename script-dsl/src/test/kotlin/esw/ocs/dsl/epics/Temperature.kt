package esw.ocs.dsl.epics

import akka.Done
import akka.actor.typed.SpawnProtocol
import akka.stream.ActorMaterializer
import csw.event.api.javadsl.IEventService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores
import csw.location.api.javadsl.ILocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.params.events.Event
import esw.ocs.dsl.compareTo
import esw.ocs.dsl.params.intKey
import io.lettuce.core.RedisClient
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.seconds

abstract class TestMachine(name: String, init: String) : Machine(name, init) {
    private var database = mutableMapOf<String, Event>()


    val system = ActorSystemFactory.remote(SpawnProtocol.behavior())
    val mat = ActorMaterializer.apply(null, null)
    val locationService: ILocationService = JHttpLocationServiceFactory.makeLocalClient(system, mat)
    val redisClient = RedisClient.create()
    val eventServiceFactory: EventServiceFactory = object : EventServiceFactory(EventStores.RedisStore(redisClient))
    val service: IEventService = eventServiceFactory.jMake(locationService, system)

    override val eventService: IEventService
        get() = service
//    TODO("not implemented") // To change initializer of created properties use File | Settings | File Templates.

    override suspend fun publishEvent(event: Event): Done {
        database[event.eventKey().key()] = event
        return Done.done()
    }

    override suspend fun getEvent(vararg eventKeys: String): Set<Event> =
        eventKeys.mapNotNull { database[it] }.toSet()

    val prefix = "esw.epic"
    val tempKey = intKey("temp")
}


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
    var temp = createVar(0, "$prefix.temp", tempKey)

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
<<<<<<< HEAD
    println("============= MACHINE 1 =============")
    machine1.refresh("Init")
=======
    machine3.refresh("Init")
>>>>>>> Adding machine using pvMonitor()

    println()
    println("============= MACHINE 2 =============")
    machine2.refresh("Init")
}
