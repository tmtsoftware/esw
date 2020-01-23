# Location Service

Location Service DSL is a wrapper over Location Service module provided by CSW.
You can refer a detailed documentation of Location Service provided by CSW @extref[here](csw:services/location).

This DSL provides following APIs:

## register

This DSL registers provided `Registration` with Location Service and returns `RegistrationResult` which contains `Location` with which component is registered and handle for un-registration.

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

This DSL look up for provided `Connection` in Location Service and returns corresponding `Location` or `null` if not found.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #find-location }

## resolveLocation

This DSL keeps looking for provided `Connection` in Location Service for the provided/default duration and returns corresponding `Location` or `null` if not found after duration exhausts.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #resolve-location }

## listLocations

Lists all the locations currently registered with the Location Service.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations }

## listLocationsBy

Following various APIs are provided for listing locations with filtering criteria:

### @extref[ComponentType](csw_scaladoc:csw/location/models/ComponentType)

Filters locations based on provided `ComponentType`, for example, HCD, Assembly, Sequencer etc.

Following example demonstrate a use case where script sends Offline command to all the Assemblies.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-comp-type }

### @extref[ConnectionType](csw_scaladoc:csw/location/models/ConnectionType)

Filters locations based on provided `ConnectionType`, for example, AkkaType, HttpType, TcpType etc.

Following example demonstrate a use case where script locks all the components i.e. Assemblies and HCD's.

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-connection-type }

@@@ note

Following import is required for creating `ConnectionType` and `ComponentType`

```kotlin
import esw.ocs.dsl.highlevel.models.*
```

@@@

### Hostname

Filters locations based on provided hostname.

In the following example, we are listing all the components running on `IRIS` (hostname: **10.1.1.1**) machine

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-hostname }

### Prefix

Filters locations based on provided Prefix.

In the following example, we are listing all the componenst registered using Prefix: `IRIS.filter.wheel` where `IRIS` is a Subsystem and `filter.wheel` is a component name

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #list-locations-by-prefix }

## onLocationTrackingEvent

This DSL allows you to add a callback on every location changed event which is represented by `TrackingEvent`.

@extref[TrackingEvent](csw_scaladoc:csw/location/models/TrackingEvent) has following two subclasses

1. @extref[LocationUpdated](csw_scaladoc:csw/location/models/LocationUpdated): Published when location is registered with Location Service

1. @extref[LocationRemoved](csw_scaladoc:csw/location/models/LocationRemoved): Published when location is removed from Location Service

Kotlin
:   @@snip [LocationServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts) { #on-location-tracking-event }

@@@ note

`sendUIEvent` used in above examples is just for demonstration purpose and is not part of DSL.
`sendUIEvent` publish `SystemEvent` with provided message.

@@@

## Source code for examples

* [Location Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/LocationServiceDslExample.kts)
