@file:Suppress("unused", "UNUSED_ANONYMOUS_PARAMETER", "UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import csw.database.javadsl.JooqHelper
import esw.ocs.dsl.core.script
import kotlinx.coroutines.future.await
import org.jooq.impl.DSL.field
import org.jooq.impl.DSL.name

script {

    //#dsl-jooq-helper
    val readDslContext = makeDatabaseService(dbName = "IRIS_db")

    onSetup("setup-iris") {
        val query = readDslContext
                .resultQuery("SELECT filter_key FROM filter_table")

        //await to get result of query to achieve sequential flow of execution
        val filterKeys = JooqHelper.fetchAsync(query, String::class.java).await()

        // do something with filter keys
    }
    //#dsl-jooq-helper

    //#dsl-context-with-write-access
    val context = makeDatabaseService("IRIS_db", "db_write_username", "db_write_password")

    onObserve("command-1") {
        val result = context
                .select(field("event_name"))
                .from(name("table_1")).fetch()

        // do something with result
    }
    //#dsl-context-with-write-access
}
