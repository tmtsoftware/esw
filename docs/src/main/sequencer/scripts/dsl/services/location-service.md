# Location Service

The Location Service DSL is a wrapper over the Location Service module provided by CSW.
You can refer the detailed documentation of the Location Service provided by CSW @extref[here](csw:services/location).

This DSL provides the following APIs:

## register

This DSL registers a provided `Registration` with the Location Service and returns a `RegistrationResult`, which contains the `Location` with which a component is registered and a handle for un-registration.

@extref[Registration](csw_scaladoc:csw/location/models/Registration) can be one of:

1. @extref[AkkaRegistration](csw_scaladoc:csw/location/models/AkkaRegistration)
1. @extref[HttpRegistration](csw_scaladoc:csw/location/models/HttpRegistration)
1. @extref[TcpRegistration](csw_scaladoc:csw/location/models/TcpRegistration)

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #register }

## unregister

This DSL un-registers provided `Connection` from Location Service

@extref[Connection](csw_scaladoc:csw/location/models/Connection) can be one of:

1. @extref[AkkaConnection](csw_scaladoc:csw/location/models/Connection$$AkkaConnection)
1. @extref[HttpConnection](csw_scaladoc:csw/location/models/Connection$$HttpConnection)
1. @extref[TcpConnection](csw_scaladoc:csw/location/models/Connection$$TcpConnection)

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #unregister }

## findLocation

This DSL looks up the provided `Connection` in the Location Service and returns the corresponding `Location`, or `null`, if not found.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #find-location }

## resolveLocation

This DSL looks for the specified `Connection` in the Location Service for the optionally specified duration (default is 5 seconds) and returns the corresponding `Location`.
If the location is not resolved within the time limit, `null` is returned.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #resolve-location }

## listLocations

Lists all the locations currently registered with the Location Service.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations }

## listLocationsBy

The following various APIs are provided for listing locations with filtering criteria:

### @extref[ComponentType](csw_scaladoc:csw/location/models/ComponentType)

Filters locations based on provided `ComponentType` (HCD, Assembly, Sequencer, etc.)

The following example demonstrates a use case where the script sends an Offline command to all of the Assemblies currently registered.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-comp-type }

### @extref[ConnectionType](csw_scaladoc:csw/location/models/ConnectionType)

Filters locations based on provided `ConnectionType` (AkkaType, HttpType, TcpTyp,e etc.)

The following example demonstrates a use case where the script locks all of the components i.e. Assemblies and HCD's.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-connection-type }

@@@ note

The following import is required for using `ConnectionType` and `ComponentType`:

```kotlin
import esw.ocs.dsl.highlevel.models.*
```

@@@

### Hostname

Filters locations based on provided hostname.

In the following example, all of the components running on `IRIS` (hostname: **10.1.1.1**) machine are listed.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-hostname }

### Prefix

Filters locations based on provided Prefix.

In the following example, all of the components registered using the Prefix: `IRIS.filter.wheel` are listed, where `IRIS` is a Subsystem and `filter.wheel` is a component name

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-prefix }

## onLocationTrackingEvent

This DSL allows you to add a callback that is called when the location of the specified connection changes, which is represented by a `TrackingEvent`.

@extref[TrackingEvent](csw_scaladoc:csw/location/models/TrackingEvent) has following two subclasses

1. @extref[LocationUpdated](csw_scaladoc:csw/location/models/LocationUpdated): Published when the location is registered with the Location Service

1. @extref[LocationRemoved](csw_scaladoc:csw/location/models/LocationRemoved): Published when the location is removed from the Location Service

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #on-location-tracking-event }

@@@ note

`sendUIEvent` used in above examples is just for demonstration purposes and is not part of DSL.
The `sendUIEvent` method, defined elsewhere in the script, publishes a `SystemEvent` with the provided message.

@@@

## Source code for examples

* [Location Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts)
