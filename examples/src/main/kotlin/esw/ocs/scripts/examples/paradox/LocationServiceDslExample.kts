/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.location.api.models.*
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.Connection.HttpConnection
import csw.prefix.models.Prefix
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.*
import esw.ocs.dsl.params.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Duration.Companion.minutes

script {

    val msgKey = stringKey("ui-event")
    suspend fun sendUIEvent(msg: String) = publishEvent(SystemEvent("ESW.ui", "ui-event", msgKey.set(msg)))

    onSetup("spawn-service") { cmd ->
        val portKey = intKey("port")
        val port = cmd.params(portKey).first
        val prefix = Prefix("IRIS.filter.wheel")
        val httpConnection = HttpConnection(ComponentId(prefix, Service))

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
        val componentId = ComponentId(sourcePrefix, Service)

        unregister(HttpConnection(componentId))
        //#unregister
    }

    onSetup("find") { cmd ->
        //#find-location
        val prefix: Prefix = cmd.source()
        val assemblyConnection = AkkaConnection(ComponentId(prefix, Assembly))

        val location: AkkaLocation? = findLocation(assemblyConnection)

        // send a successful event to UI if assembly location is found
        location?.let { sendUIEvent("Resolved assembly location: $it") }
        //#find-location
    }

    onSetup("resolve") { cmd ->
        //#resolve-location
        val prefix: Prefix = cmd.source()
        val assemblyConnection = AkkaConnection(ComponentId(prefix, Assembly))

        val location: AkkaLocation? = resolveLocation(assemblyConnection, 10.seconds)

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
        val assemblyLocations: List<Location> = listLocationsBy(Assembly)

        // create Assemblies from locations and send offline command to each one of them
        val assemblies = assemblyLocations.map { Assembly(it.prefix, 10.minutes) }
        assemblies.forEach { it.goOffline() }
    }
    //#list-locations-by-comp-type

    //#list-locations-by-connection-type
    onSetup("lock-all-components") {
        val timeout = 10.minutes
        val leaseDuration = 20.minutes

        // list all akka components
        val akkaLocations: List<Location> = listLocationsBy(AkkaType)

        // filter HCD's and Assemblies and send Lock command
        akkaLocations.forEach { location ->
            val compId: ComponentId = location.connection.componentId
            val compType: ComponentType = compId.componentType
            val prefix = location.prefix

            // create Assembly or Hcd instance based on component type and send Lock command
            when (compType) {
                Assembly -> Assembly(prefix, timeout).lock(leaseDuration)

                HCD -> Hcd(prefix, timeout).lock(leaseDuration)

                else -> warn("Unable to lock component $compId, Invalid component type $compType")
            }
        }
    }
    //#list-locations-by-connection-type

    onSetup("prefix-hostname") {
        //#list-locations-by-hostname
        // list all the components running on IRIS machine
        val irisMachineHostname = "10.1.1.1"
        val irisMachineLocations: List<Location> = listLocationsByHostname(irisMachineHostname)

        sendUIEvent("IRIS machine running components: [$irisMachineLocations]")
        //#list-locations-by-hostname

        //#list-locations-by-prefix
        val irisPrefixString ="IRIS.filter."
        val irisComponents: List<Location> = listLocationsBy(irisPrefixString)

        // log Assembly and HCD location
        irisComponents.forEach {
            when (it.connection.componentId.componentType) {
                Assembly -> info("Assembly starting with $irisPrefixString is registered with location: $it")

                HCD -> info("HCD starting with $irisPrefixString is registered with location: $it")

                else -> error("Invalid location: $it found for $irisPrefixString")
            }
        }
        //#list-locations-by-prefix
    }

    //#on-location-tracking-event
    onObserve("monitor-iris-sequencer") {
        val irisPrefix = Prefix("IRIS.darknight")
        val irisComponent = ComponentId(irisPrefix, Sequencer)
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
