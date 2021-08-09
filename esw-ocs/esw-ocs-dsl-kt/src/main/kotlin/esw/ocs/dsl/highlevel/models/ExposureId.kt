package esw.ocs.dsl.highlevel.models

import csw.params.core.models.*
import csw.prefix.models.Subsystem

// ******** Helpers to access values from ExposureId ********
val ExposureId.obsId: ObsId? get() = obsId().getOrElse(null)
val ExposureId.det: String get() = det()
val ExposureId.subsystem: Subsystem get() = subsystem()
val ExposureId.typLevel: TYPLevel get() = typLevel()
val ExposureId.exposureNumber: ExposureNumber get() = exposureNumber()

// *********** Helpers to create models for ExposureId *************
fun TYPLevel(value: String): TYPLevel = TYPLevel.apply(value)
fun ExposureNumber(value: String): ExposureNumber = ExposureNumber.apply(value)
fun ExposureId(value: String): ExposureId = ExposureId.fromString(value)
