/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel

import csw.database.DatabaseServiceFactory
import csw.location.api.javadsl.ILocationService
import kotlinx.coroutines.future.await
import org.jooq.DSLContext

interface DatabaseServiceDsl {

    val databaseServiceFactory: DatabaseServiceFactory
    val locationService: ILocationService

    /**
     * Creates an instance of DSLContext using csw databaseServiceFactory
     * with given dbName, default username holder(env variable) [csw.database.DatabaseServiceFactory.ReadUsernameHolder]
     * and default password holder(env variable) [csw.database.DatabaseServiceFactory.ReadPasswordHolder]
     *
     * @param dbName - database name where the query will be made by returned DSLContext
     *
     * @return a [[org.jooq.DSLContext]]
     */
    suspend fun makeDatabaseService(dbName: String): DSLContext =
            databaseServiceFactory.jMakeDsl(locationService, dbName).await()

    /**
     * Creates an instance of DSLContext using csw databaseServiceFactory
     * with given dbName, given username holder(env variable)
     * and given password holder(env variable)
     *
     * @param dbName - database name where the query will be made by returned DSLContext
     * @param usernameHolder - name of environment variable of the username to be used to query
     * @param passwordHolder - name of environment password of the username to be used to query
     *
     * @return a [[org.jooq.DSLContext]]
     */
    suspend fun makeDatabaseService(dbName: String, usernameHolder: String, passwordHolder: String): DSLContext =
            databaseServiceFactory.jMakeDsl(locationService, dbName, usernameHolder, passwordHolder).await()

}
