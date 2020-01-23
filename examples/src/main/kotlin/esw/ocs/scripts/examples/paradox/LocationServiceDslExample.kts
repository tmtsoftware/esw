@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.location.api.javadsl.JComponentType
import csw.location.api.javadsl.JConnectionType
import csw.location.models.*
import csw.location.models.Connection.AkkaConnection
import csw.location.models.Connection.HttpConnection
import csw.prefix.models.Prefix
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.Prefix
import esw.ocs.dsl.highlevel.models.RegistrationResult
import esw.ocs.dsl.params.*
import kotlin.time.minutes

script {
    val prefix = Prefix("IRIS.filter.wheel")
    val componentType = JComponentType.Service()
    val componentId = ComponentId(prefix, componentType)
    val httpConnection = HttpConnection(componentId)

    val msgKey = stringKey("ui-event")
    suspend fun sendUIEvent(msg: String) = publishEvent(ObserveEvent("ESW.ui", "ui-event", msgKey.set(msg)))

    onSetup("spawn-service") { cmd ->
        val portKey = intKey("port")
        val port = cmd.params(portKey).first

        //#register
        // register HTTP service running at port 8080 and routes are served from /routes endpoint
        val registrationResult: RegistrationResult =
                register(HttpRegistration(httpConnection, port, "/routes"))

        // location which is registered with Location Service
        val location: Location = registrationResult.location

        // unregisters location from Location Service which triggers LocationRemoved event
        registrationResult.unregister()
        //#register
    }

    onSetup("stop-service") { cmd ->
        //#unregister
        val sourcePrefix: Prefix = cmd.source()
        val componentId = ComponentId(sourcePrefix, JComponentType.Service())

        unregister(HttpConnection(componentId))
        //#unregister
    }

    onSetup("find") { cmd ->
        //#find-location
        val prefix: Prefix = cmd.source()
        val assemblyConnection = AkkaConnection(ComponentId(prefix, JComponentType.Assembly()))

        val location: AkkaLocation? = findLocation(assemblyConnection)

        // send a successful event to UI if assembly location is found
        location?.let { sendUIEvent("Resolved assembly location: $it") }
        //#find-location
    }

    onSetup("resolve") { cmd ->
        //#resolve-location
        val prefix: Prefix = cmd.source()
        val assemblyConnection = AkkaConnection(ComponentId(prefix, JComponentType.Assembly()))

        val location: AkkaLocation? = resolveLocation(assemblyConnection)

        // send a successful event to UI if assembly location is found
        location?.let { sendUIEvent("Resolved assembly location: $it") }
        //#resolve-location
    }

    onObserve("location-metrics") {
        //#list-locations
        val allLocations: List<Location> = listLocations()

        // publish all locations to UI in the format of [location1, location2, ...]
        sendUIEvent(allLocations.joinToString(prefix = "[", postfix = "]"))
        //#list-locations
    }

    //#list-locations-by-comp-type
    onSetup("offline-assemblies") {
        // list all Assembly components
        val assemblyLocations: List<Location> = listLocationsBy(JComponentType.Assembly())

        // create Assemblies from locations and send offline command to each one of them
        val assemblies = assemblyLocations.map { Assembly(it.prefix().toString(), 10.minutes) }
        assemblies.forEach { it.goOffline() }
    }
    //#list-locations-by-comp-type

    //#list-locations-by-connection-type
    onSetup("lock-all-components") {
        val timeout = 10.minutes
        val leaseDuration = 20.minutes

        // list all akka components
        val akkaLocations: List<Location> = listLocationsBy(JConnectionType.AkkaType())

        // filter HCD's and Assemblies and send Lock command
        akkaLocations.forEach { location ->
            val compId: ComponentId = location.connection().componentId()
            val compType: ComponentType = compId.componentType()
            val prefix = location.prefix().toString()

            // create Assembly or Hcd instance based on component type and send Lock command
            when (compType) {
                JComponentType.Assembly() -> Assembly(prefix, timeout).lock(leaseDuration)

                JComponentType.HCD() -> Hcd(prefix, timeout).lock(leaseDuration)

                else -> warn("Unable to lock component $compId, Invalid component type $compType")
            }
        }
    }
    //#list-locations-by-connection-type

    onSetup("prefix-hostname") {
        //#list-locations-by-hostname
        // list all the components running on IRIS machine
        val irisMachineHostname = "10.1.1.1"
        val irisMachineLocations: List<Location> = listLocationsBy("10.1.1.1")

        sendUIEvent("IRIS machine running components: [$irisMachineLocations]")
        //#list-locations-by-hostname

        //#list-locations-by-prefix
        val irisPrefix = Prefix("IRIS.filter.wheel")
        val irisComponents: List<Location> = listLocationsBy(irisPrefix)

        // log Assembly and HCD location
        irisComponents.forEach {
            when (it.connection().componentId().componentType()) {
                JComponentType.Assembly() -> info("$irisPrefix is registered as Assembly with location: $it")

                JComponentType.HCD() -> info("$irisPrefix is registered as HCD with location: $it")

                else -> error("Invalid location: $it found for $irisPrefix")
            }
        }
        //#list-locations-by-prefix
    }

    //#on-location-tracking-event
    onObserve("monitor-iris-sequencer") {
        val irisPrefix = Prefix("IRIS.darknight")
        val irisComponent = ComponentId(irisPrefix, JComponentType.Sequencer())
        val irisSequencerConnection = AkkaConnection(irisComponent)

        // send UI events on iris sequencers location change
        onLocationTrackingEvent(irisSequencerConnection) {
            when (it) {
                is LocationUpdated ->
                    sendUIEvent("[INFO] Location updated ${it.location()}")

                is LocationRemoved ->
                    sendUIEvent("[ERROR] Location removed for connection: ${it.connection()}")
            }
        }
    }
    //#on-location-tracking-event
}