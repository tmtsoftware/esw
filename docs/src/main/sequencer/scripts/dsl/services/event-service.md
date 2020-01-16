# Event Service

Event Service DSL is kotlin wrapper over CSW Event Service. This DSL have the ability of publishing, subscribing to events and holds some helper methods.
You can refer a detailed documentation of Event Service provided by CSW @extref[here](csw:services/event).

This DSL exposes following APIs:

## publishEvent

DSL to publish the given `event`.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #publish } 

This DSL can also publish events periodically when provided with `duration` and `event generator` function.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #publish-async }
 
## onEvent

DSL to subscribe to events getting published on the given `event keys`. This DSL takes a `callback` which operates on an event, that block will be invoked whenever an
event is published on any of the provided event keys.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #subscribe }

This DSL have the ability to limit the number of events by providing the `duration` after which the `callback` will be invoked with the latest event. If no event is 
published within the duration then the last published event will be used to execute the callback. 

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #subscribe-async } 

## getEvent

DSL to get the latest event published on each of the given `event keys`

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #get-event }

## HelperMethods

### EventKey

Helper DSL to create `EventKey` model from a full `event key string` or using `prefix` and `event name` strings. Example demos both methods to create EventKey.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #event-key }

### SystemEvent

Helper DSL to create `SystemEvent` from the provided `prefix`, `event name` and `parameters` (optional).

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #system-event }

### ObserveEvent

Helper DSL to create `ObserveEvent` from the provided `prefix`, `event name` and `parameters` (optional).

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #observe-event }

### SystemVar

Helper DSL to create `EventVariable` corresponding to `SystemEvent`. This DSL needs the `initial value` of the parameter, `event key` and `parameter key`. 
 More details about SystemVar are provided in @ref[FSM documentation](./../../fsm.md#reactive-fsm)

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #system-var }

### ObserveVar

Helper DSL to create `EventVariable` corresponding to `ObserveEvent`. This DSL needs the `initial value` of the parameter, `event key` and `parameter key`.
More details about ObserveVar are provided in @ref[FSM documentation](./../../fsm.md#reactive-fsm)

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #observe-var }


## Source code for examples
* [Event Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts)
