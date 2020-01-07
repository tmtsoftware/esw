# Time Service Dsl

Time Service Dsl is dsl wrapper over time service module provided by csw. This dsl exposes following APIs to script writers to
schedule tasks at given time. It also exposes utility methods for getting specified utc time or tai time and calculate offset. 

## utcTimeNow

This utility provides current utc time.
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #utc-time-now }


## taiTimeNow

This utility provides current utc time.
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #tai-time-now }


## utcTimeAfter

This utility provides utc time after provided duration. Following example shows how to get utc time after 1 hour
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #utc-time-after }


## taiTimeAfter

This utility provides tai time after provided duration. Following example shows how to get tai time after 1 hour
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #tai-time-after }


## scheduleOnce

This API allows to schedule non periodic task in script at specified utc time or tai time.
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #schedule-once }

## scheduleOnceFromNow

This API allows to schedule non periodic task in script after specified duration. This API takes time duration after which task will
be scheduled. scheduleOnceFromNow internally creates instance of utc time considering specified in duration. Following example shows
scheduling task after 1 hour from current utc time.
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #schedule-once-from-now }

## schedulePeriodically

This API allows to schedules a task to execute periodically at the given interval. The task is executed once at the given start time followed by execution of task at each interval. 
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #schedule-periodically }


## schedulePeriodicallyFromNow

This API allows to schedules a task to execute periodically at the given interval. This API takes time duration after which task will
be scheduled once followed by execution of task at each interval. Following example shows scheduling task after 1 hour from current utc time
and then executing it periodically at 10 seconds interval.
Usage:
Kotlin
:   @@snip [timeServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts) { #schedule-periodically-from-now }

# Source code for above examples

* @github[Time Service Example Script](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/timeServiceDslExample.kts)
