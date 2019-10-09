package esw.ocs.dsl.highlevel

import esw.ocs.dsl.script.StrandEc

interface CswHighLevelDsl : EventServiceDsl, AlarmServiceDsl, TimeServiceDsl, CommandServiceDsl, CrmDsl, DiagnosticDsl,
    LockUnlockDsl, OnlineOfflineDsl {
    fun strandEc(): StrandEc
    override val commonUtils: CommonUtils
}
