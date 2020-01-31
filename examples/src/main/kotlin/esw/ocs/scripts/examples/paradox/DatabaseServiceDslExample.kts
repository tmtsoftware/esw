@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.database.javadsl.JooqHelper
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.intKey
import esw.ocs.dsl.params.stringKey
import kotlinx.coroutines.future.await
import scala.collection.immutable.Seq

class Data(val counter: Int)

script {

    val diagnostic_database = makeDatabaseService("diagnostic_data")

    onDiagnosticMode {startTime, hint ->
        val query = diagnostic_database
                .resultQuery("SELECT datuming_data FROM filter_wheel_data")

        val list = JooqHelper.fetchAsync(query, String::class.java).await()


    }
}
