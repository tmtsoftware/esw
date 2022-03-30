/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel

import csw.database.DatabaseServiceFactory
import csw.location.api.javadsl.ILocationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import java.util.concurrent.CompletableFuture

@Suppress("DANGEROUS_CHARACTERS")
class DatabaseServiceDslTest : DatabaseServiceDsl {
    override val databaseServiceFactory: DatabaseServiceFactory = mockk()
    override val locationService: ILocationService = mockk()

    private val dslContext: DSLContext = mockk()
    private val dbName = "tmt-db"
    private val usernameHolder = "username-holder"
    private val passwordHolder = "password-holder"

    @Test
    fun `makeDsl should be able to call databaseServiceFactory#makeDsl(dbName) | ESW-124`() = runBlocking {

        every { databaseServiceFactory.jMakeDsl(locationService, dbName) }.answers { CompletableFuture.completedFuture(dslContext) }

        makeDatabaseService(dbName)
        verify { databaseServiceFactory.jMakeDsl(locationService, dbName) }
    }

    @Test
    fun `makeDsl should be able to call databaseServiceFactory#makeDsl(dbName, usernameHolder, passwordHolder) | ESW-124`() = runBlocking {

        every { databaseServiceFactory.jMakeDsl(locationService, dbName, usernameHolder, passwordHolder) }.answers { CompletableFuture.completedFuture(dslContext) }

        makeDatabaseService(dbName, usernameHolder, passwordHolder)
        verify { databaseServiceFactory.jMakeDsl(locationService, dbName, usernameHolder, passwordHolder) }
    }
}