package esw.ocs.dsl.highlevel.models

import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem

fun Prefix(subsystem: Subsystem, componentName: String): Prefix = Prefix.apply(subsystem, componentName)
fun Prefix(value: String): Prefix = Prefix.apply(value)