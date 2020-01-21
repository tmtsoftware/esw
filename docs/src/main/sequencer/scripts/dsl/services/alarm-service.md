# Alarm Service

Alarm Service DSL is a wrapper over Alarm Service module provided by CSW.
You can refer a detailed documentation of Alarm Service provided by CSW @extref[here](csw:services/alarm).

This DSL provides an API to set the severity of alarm.

## setSeverity

This API sets alarm severity for alarm key to provided value and keeps refreshing it in the background with the interval of config value `csw-alarm.refresh-interval`.
Default value for `csw-alarm.refresh-interval` config is _3 seconds_ which is configured in downstream CSW alarm modules `reference.conf` file.

`setSeverity` API requires user to provide `AlarmKey` and `AlarmSeverity`.

### AlarmKey

`AlarmKey` represents unique alarm in the given subsystem and component e.g. `nfiraos.trombone.tromboneaxislowlimitalarm`

Following example demonstrate creation of `AlarmKey`

Kotlin
:   @@snip [AlarmServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/AlarmServiceDslExample.kts) { #alarm-key }

### AlarmSeverity

Supported `AlarmSeverity` are:

1. Okay
1. Warning
1. Major
1. Indeterminate
1. Disconnected
1. Critical

Following example demonstrate the usage of `setSeverity` API.
In this example, temperature @ref[Fsm](../../fsm.md) is created and based on the state of fsm, severity is set accordingly.

| State |       Temperature      | Severity |
|:-----:|:----------------------:|:--------:|
| OK    | Temp > 20 && temp < 40 | Okay     |
| ERROR | Temp < 20 or temp > 40 | Major    |

Kotlin
:   @@snip [AlarmServiceDslExample.kts](../../../../../../../examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/AlarmServiceDslExample.kts) { #set-severity }

## Source code for examples

* [Alarm Service Examples]($github.base_url$/examples/src/main/kotlin/esw/ocs/scripts/examples/paradox/AlarmServiceDslExample.kts)
