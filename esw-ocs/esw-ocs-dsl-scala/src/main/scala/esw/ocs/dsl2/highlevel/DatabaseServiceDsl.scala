package esw.ocs.dsl2.highlevel

import csw.database.DatabaseServiceFactory
import csw.location.api.scaladsl.LocationService
import org.jooq.DSLContext

import async.Async.*
import scala.concurrent.ExecutionContext

class DatabaseServiceDsl(
    databaseServiceFactory: DatabaseServiceFactory,
    locationService: LocationService
)(using ExecutionContext) {

  inline def makeDatabaseService(dbName: String): DSLContext =
    await(databaseServiceFactory.makeDsl(locationService, dbName))

  inline def makeDatabaseService(dbName: String, usernameHolder: String, passwordHolder: String): DSLContext =
    await(databaseServiceFactory.makeDsl(locationService, dbName, usernameHolder, passwordHolder))

}
