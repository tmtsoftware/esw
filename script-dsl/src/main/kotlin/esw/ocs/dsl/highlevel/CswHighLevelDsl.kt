package esw.ocs.dsl.highlevel

import esw.ocs.macros.StrandEc

interface CswHighLevelDsl : EventServiceKtDsl, LocationServiceKtDsl,
    TimeServiceKtDsl, CommandServiceKtDsl, CrmKtDsl {
    fun strandEc(): StrandEc
}
