package esw.ocs.dsl.highlevel

import esw.ocs.dsl.script.StrandEc

interface CswHighLevelDsl : EventServiceDsl, TimeServiceDsl, CommandServiceDsl, CrmDsl, DiagnosticDsl, LockUnlockDsl {
    fun strandEc(): StrandEc
}
