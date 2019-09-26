package esw.ocs.dsl.highlevel

import esw.ocs.macros.StrandEc

interface CswHighLevelDsl : EventServiceDsl, LocationServiceDsl,
    TimeServiceDsl, CommandServiceDsl, CrmDsl, DiagnosticDsl {
    fun strandEc(): StrandEc
}
