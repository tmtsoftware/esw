# Time Service

Time Service DSL is DSL wrapper over Time Service module provided by CSW. This DSL exposes following APIs to script writers to
schedule tasks at given time. It also exposes utility methods for getting specified utc time or tai time and calculate offset. 

## utcTimeNow

This utility provides current utc time.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #utc-time-now }


## taiTimeNow

This utility provides current utc time.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #tai-time-now }


## utcTimeAfter

This utility provides utc time after provided duration. Following example shows how to get utc time after 1 hour

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #utc-time-after }


## taiTimeAfter

This utility provides tai time after provided duration. Following example shows how to get tai time after 1 hour

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #tai-time-after }


## scheduleOnce

This API allows scheduling non periodic task in script at specified utc time or tai time. This returns a handle to cancel the execution of the task if it hasn't been executed already.
Task is a callback which will be executed in thread safe way.

Following example shows onObserve handler of Sequencer is extracting schedule time from received observe command.
It is creating probe command which is then submitted to downstream galil Assembly at scheduled time.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-once }

## scheduleOnceFromNow

This API allows scheduling non periodic task in script after specified duration. Task is a callback which will be executed in thread safe way. 
This API takes time duration after which task will be scheduled. scheduleOnceFromNow internally creates instance of utc time considering specified in duration.
Following example shows scheduling task after 1 hour from current utc time. This returns a handle to cancel the execution of the task if it hasn't
been executed already.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-once-from-now }

## schedulePeriodically

This API allows to schedule a task to execute periodically at the given interval. Task is a callback which will be executed in thread safe way.
The task is executed once at the given start time followed by execution of task at each interval. This returns a handle to cancel the execution of further tasks.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-periodically }


## schedulePeriodicallyFromNow

This API allows to schedule a task to execute periodically at the given interval. Task is a callback which will be executed in thread safe way.
This API takes time duration after which task will be scheduled once followed by execution of task at each interval. 
Following example shows scheduling task after 1 hour from current utc time and then executing it periodically at 10 seconds interval.
This returns a handle to cancel the execution of further tasks.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-periodically-from-now }

# Source code for above examples

* [Time Service Example Script]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts)
