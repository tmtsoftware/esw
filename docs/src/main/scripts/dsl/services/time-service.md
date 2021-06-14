# Time Service Access in Scripts

The time and scheduling functionality of the CSW Time Service are available to script writers. Time Service DSL provides access to the 
Time Service module provided by CSW. This DSL exposes the following API calls to script writers to access time and schedule tasks.

### Time Access

Time Service provides access to both TAI and UTC time that is synchronized at the telescope site with time on all the other computers
and also with absolute time provided by a GPS system.

Time access calls are made available to all scripts. Access to some time functionality requires the import of `kotlin.time` packages. 

### Access UTC Time with utcTimeNow

The `utcTimeNow` Time Service utility returns the current UTC time. The time value returned is a CSW `UTCTime` type, which is an absolute time value.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #utc-time-now }

### Access TAI Time with taiTimeNow

The `taiTimeNow` Time Service utility returns the current TAI time. The time value returned is a CSW `TAITime` type, which is an absolute time value.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #tai-time-now }

### Access UTC time in the Future with utcTimeAfter

The `utcTimeAfter` Time Service utility provides an absolute UTC time some amount of time from now in the future. The value
provided is a duration such as "1 hour". The returned value is an absolute `UTCTime` type. The following example shows provides UTC time 1 hour from now.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #utc-time-after }

### Access TAI Time in the Future with taiTimeAfter

The `taiTimeAfter` Time Service utility provides an absolute TAI time some amount of time from now in the future. The value
provided is a duration. The returned value is an absolute `TAITime` type. The following example shows provides TAI time 1 hour from now.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #tai-time-after }

### Scheduling with Time Service

The Time Service DSL provides script access to the CSW scheduling library allowing scripts to schedule one-off and periodic tasks.
Access to the Time Service scheduling functionality is provided in the script writing environment with no extra imports.

### Using scheduleOnce

The `scheduleOnce` Time Service DSL allows scheduling a task in a script once at the specified absolute UTC or TAI time. The function schedules the task and 
returns a handle that can be used to cancel the execution of the task if it has not yet executed. 
The task is a callback which will be executed in thread safe way.

The following example shows an onSetup handler of a script extracting a scheduled time from a Setup command and then uses
 the scheduleOnce to send a motion command to a Galil Assembly at the scheduled time.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-once }

### Using scheduleOnceFromNow

Often, it is necessary to schedule a task in the future some amount of time from now.
The `scheduleOnceFromNow` API allows scheduling non-periodic task in script after a specified duration. The task is a callback which will be 
executed in thread-safe way. This API takes a time `Duration` type after which task will be scheduled.

The following example shows the scheduling of a task after 1 hour from now. The function takes a duration and returns a handle 
which can be used to cancel the execution of the task if it has not yet executed.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-once-from-now }

### Using schedulePeriodically

The `schedulePeriodically` API allows scheduling a task to execute periodically at a given interval. It is also possible to provide the start time otherwise the task will be 
executed immediately. The provided task is a callback, which will be executed in thread-safe way.

Initially, the task is executed once immediately / on start time and then followed by periodic execution of the task at the requested period. 
This function returns a handle that can be used to cancel the execution of further tasks.

The following examples show the scheduling of a task in `onSetup` handlers. The second usage demos using time provided in a parameter as start time. Tasks run
every 5 seconds after *start time* until stopped.

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-periodically }

### Using schedulePeriodicallyFromNow

The `schedulePeriodicallyFromNow` API is like `schedulePeriodically` but takes a duration as the start time rather than an absolute time. 
Initially, the task is executed once after a delay from the current time specified by the duration time. This execution is followed by periodic execution of the task at the requested period. 
The task is a callback which will be executed in thread-safe way. This function returns a handle that can be used to cancel the execution of further tasks.

The following example shows scheduling the publishing of an Event after 1 hour from now and then publishes an Event periodically with a 10 second period
until cancelled. 

Kotlin
:   @@snip [TimeServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts) { #schedule-periodically-from-now }

@@@ note { title="Limits of Scheduling" }

The script environment for scheduling tasks should not be relied upon for very short periods or low jitter applications.  

@@@

## Source code for above examples

* [Time Service Example Script]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/TimeServiceDslExample.kts)
