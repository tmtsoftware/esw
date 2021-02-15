package esw.ocs.dsl.highlevel.models

import csw.params.core.models.ObsId
import csw.params.core.models.ProgramId
import csw.params.core.models.Semester
import csw.params.core.models.SemesterId
import java.time.Year


// ******** Helpers to access values from ObsId ********
val ObsId.programId: ProgramId get() = programId()
val ObsId.observationNumber: Int get() = observationNumber()

// ******** Helpers to access values from ProgramId ********
val ProgramId.semesterId: SemesterId get() = semesterId()
val ProgramId.programNumber: Int get() = programNumber()

// ******** Helpers to access values from SemesterId ********
val SemesterId.year: Year get() = year()
val SemesterId.semester: Semester get() = semester()

// *********** Helpers to create models for ObsId *************
fun SemesterId(value: String): SemesterId = SemesterId.apply(value)
fun ProgramId(value: String): ProgramId = ProgramId.apply(value)
fun ObsId(value: String): ObsId = ObsId.apply(value)
