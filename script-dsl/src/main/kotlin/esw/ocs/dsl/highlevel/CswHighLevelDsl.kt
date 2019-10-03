package esw.ocs.dsl.highlevel

import esw.ocs.macros.StrandEc

interface CswHighLevelDsl : EventServiceDsl, TimeServiceDsl, CommandServiceDsl, CrmDsl, DiagnosticDsl, LockUnlockDsl {
    fun strandEc(): StrandEc
}
