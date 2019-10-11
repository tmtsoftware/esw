package esw.ocs.dsl.highlevel

import csw.alarm.models.AlarmSeverity
import csw.alarm.models.Key.AlarmKey

data class AlarmSeverityData(internal val map: HashMap<AlarmKey, AlarmSeverity>)
