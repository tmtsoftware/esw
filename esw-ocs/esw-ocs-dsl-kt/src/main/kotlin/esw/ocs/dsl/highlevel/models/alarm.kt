/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

@file:Suppress("unused")

package esw.ocs.dsl.highlevel.models

import csw.alarm.api.javadsl.JAlarmSeverity
import csw.alarm.models.AlarmSeverity
import csw.alarm.models.FullAlarmSeverity

/* ******** Helpers to create AlarmSeverity ******** */
val Okay: AlarmSeverity = JAlarmSeverity.Okay
val Warning: AlarmSeverity = JAlarmSeverity.Warning
val Major: AlarmSeverity = JAlarmSeverity.Major
val Indeterminate: AlarmSeverity = JAlarmSeverity.Indeterminate
val Disconnected: FullAlarmSeverity = JAlarmSeverity.Disconnected
val Critical: AlarmSeverity = JAlarmSeverity.Critical
