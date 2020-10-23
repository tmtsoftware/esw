
# Event Service

The Event Service DSL is a Kotlin wrapper for the CSW Event Service. This DSL has the ability of publishing, getting, and subscribing to events,
and also contains some helper methods for model creation.
You can refer to the detailed documentation of Event Service provided by CSW @extref[here](csw:services/event).

## Helper Methods

These methods can be used to create Systerm and Observe Events.  Additionally, a System or Observe "Event Variable" can be created
that can be tied to the first value of a parameter of an Event, similar to the way local variables are tied to "process variables" in the EPICS State Notation
Language.

### SystemEvent

Helper DSL to create a `SystemEvent` from the provided `prefix`, `event name` and `parameters` (optional).

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #system-event }

### ObserveEvent

Helper DSL to create an `ObserveEvent` from the provided `prefix`, `event name` and `parameters` (optional).

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #observe-event }

## publishEvent

DSL to publish the given `Event`.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #publish } 

This DSL can also publish events periodically when provided with the optional `duration` and an `event generator` function. 
In the below example, an Event with temperature Key will get published every 10 seconds, with current temperature value given by *getTemperature* method.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #publish-async }

## onEvent

DSL to subscribe to events getting published on the given `EventKey` names. This DSL takes a `callback` as a lambda which 
operates on an event.  The callback block will be invoked whenever an Event is published on any of the provided event keys.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #subscribe }

This DSL has the ability to control the subscription rate by providing a `duration` with the `callback`.  This operates 
like the @extref[Rate Adapter Mode](csw:services/event#controlling-subscription-rate) for regular Event Service subscriptions.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #subscribe-async }

## getEvent

DSL to get the latest Event published on each of the given `EventKey` names. There are two variations. One is for getting the latest event for single key, the other
variation is to get the latest events against multiple keys.

Kotlin
:   @@snip [EventServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts) { #get-event }


## Source code for examples

* [Event Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/EventServiceDslExample.kts)
