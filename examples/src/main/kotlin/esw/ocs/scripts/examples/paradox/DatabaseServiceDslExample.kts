@file:Suppress("unused", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.database.javadsl.JooqHelper
import esw.ocs.dsl.core.script
import kotlinx.coroutines.future.await

class Data(val counter: Int)

script {

    val diagnostic_database = makeDatabaseService("diagnostic_data")

    onDiagnosticMode { startTime, hint ->
        val query = diagnostic_database
                .resultQuery("SELECT datuming_data FROM filter_wheel_data")

        val list = JooqHelper.fetchAsync(query, String::class.java).await()
    }
}
