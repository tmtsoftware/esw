package esw.ocs.dsl.highlevel

import csw.database.DatabaseServiceFactory
import csw.location.api.javadsl.ILocationService
import kotlinx.coroutines.future.await
import org.jooq.DSLContext

interface DatabaseServiceDsl {

    val databaseServiceFactory: DatabaseServiceFactory
    val locationService: ILocationService

    suspend fun makeDatabaseService(dbName: String): DSLContext =
            databaseServiceFactory.jMakeDsl(locationService, dbName).await()

    suspend fun makeDatabaseService(dbName: String, usernameHolder: String, passwordHolder: String): DSLContext =
            databaseServiceFactory.jMakeDsl(locationService, dbName, usernameHolder, passwordHolder).await()

}