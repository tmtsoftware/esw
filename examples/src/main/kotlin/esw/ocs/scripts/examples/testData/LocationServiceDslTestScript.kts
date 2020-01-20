package esw.ocs.scripts.examples.testData

import csw.location.api.javadsl.JComponentType
import csw.location.models.ComponentId
import csw.location.models.Connection.HttpConnection
import csw.location.models.HttpRegistration
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.Prefix
import esw.ocs.dsl.params.stringKey

script {
    val prefix = Prefix("IRIS.motor")
    val componentId = ComponentId(prefix, JComponentType.Service())
    val httpConnection = HttpConnection(componentId)
    val httpRegistration = HttpRegistration(httpConnection, 8080, "/")

    val locationEvent = SystemEvent(prefix.toString(), "location_response")
    val key = stringKey("locationResponse")

    var receivedLocationUpdated = false

    suspend fun publishLocationResponse(locationResponse: String) {
        if (locationResponse == "LocationUpdated") receivedLocationUpdated = true
        publishEvent(locationEvent.add(key.set(locationResponse)))
    }

    onSetup("track-and-register") {
        onLocationTrackingEvent(httpConnection) { event ->
            publishLocationResponse(event.javaClass.simpleName)
        }

        val registrationResult = register(httpRegistration)
        publishLocationResponse(registrationResult.javaClass.simpleName)
        waitFor { receivedLocationUpdated }
    }

    onSetup("resolve") {
        val location = resolveLocation(httpConnection) ?: finishWithError("Location not found")
        publishLocationResponse(location.javaClass.simpleName)
    }

    onSetup("list-by-prefix") {
        val locations = listLocationsBy(prefix)
        if (locations.all { it.prefix() == prefix }) // publish only if all the locations prefix matches with a prefix used for listing
            publishLocationResponse("Found = ${locations.size} Locations")
    }

    onSetup("unregister") {
        unregister(httpConnection)
        publishLocationResponse("Unregistered")
    }
}