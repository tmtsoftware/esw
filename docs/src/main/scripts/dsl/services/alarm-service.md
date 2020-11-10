# Alarm Service

The Alarm Service DSL is a wrapper for the Alarm Service module provided by CSW.
You can refer to detailed documentation of the Alarm Service provided by CSW @extref[here](csw:services/alarm).

This DSL provides an API to set the severity of an alarm. This is the component API for CSW Alarm Service.
While this API is available for special purposes, alarms should be maintained by lower-level Assemblies and HCDs, and
not in the Sequencers. This is also a problem because Sequencers do not always execute. 

## setSeverity

This API sets alarm severity for an `AlarmKey` to the provided level and keeps refreshing it in the background with the interval of config value `csw-alarm.refresh-interval`.
Default value for `csw-alarm.refresh-interval` config is _3 seconds_ which is configured in downstream CSW alarm modules `reference.conf` file.

The `setSeverity` API requires user to provide `AlarmKey` and `AlarmSeverity`.

### AlarmKey

`AlarmKey` represents unique alarm in the given subsystem and component e.g. `nfiraos.trombone.tromboneaxislowlimitalarm`

The following example demonstrates the creation of an `AlarmKey`

Kotlin
:   @@snip [AlarmServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/AlarmServiceDslExample.kts) { #alarm-key }

### AlarmSeverity

The supported `AlarmSeverity` levels are:

1. Okay
1. Warning
1. Major
1. Critical
1. Indeterminate

The following example demonstrates the usage of the `setSeverity` API.
In this example, a temperature @ref[FSM](../constructs/fsm.md) is created, and based on the state of the FSM, the severity is set accordingly.

| State |       Temperature      | Severity |
|:-----:|:----------------------:|:--------:|
| OK    | Temperature less than or equal to 40 | Okay     |
| ERROR | Temperature is greater than 40 | Major    |

Kotlin
:   @@snip [AlarmServiceDslExample.kts](../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/AlarmServiceDslExample.kts) { #set-severity }

## Source code for examples

* [Alarm Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/AlarmServiceDslExample.kts)
