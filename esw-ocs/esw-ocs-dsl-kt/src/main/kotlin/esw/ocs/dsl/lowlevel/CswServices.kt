/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.lowlevel

import akka.actor.typed.ActorSystem
import akka.actor.typed.SpawnProtocol
import csw.alarm.api.javadsl.IAlarmService
import csw.config.api.javadsl.IConfigClientService
import csw.config.client.javadsl.JConfigClientFactory
import csw.database.DatabaseServiceFactory
import csw.event.api.javadsl.IEventPublisher
import csw.event.api.javadsl.IEventService
import csw.event.api.javadsl.IEventSubscriber
import csw.location.api.javadsl.ILocationService
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.logging.api.javadsl.ILogger
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler
import esw.ocs.dsl.script.StrandEc
import esw.ocs.impl.script.ScriptContext

/**
 * An Interface which represents Collection of csw-services (Location, EventService etc.)
 *
 */
interface CswServices {

    companion object {
        /**
         * Creates an instance of CswServices
         *
         * @param ctx - an instance of ScriptContext
         * @param strandEc - an instance of strandEc (mostly single threaded execution context)
         * @return an instance of [CswServices]
         */
        internal fun create(ctx: ScriptContext, strandEc: StrandEc): CswServices = object : CswServices {
            private val actorSystem: ActorSystem<SpawnProtocol.Command> = ctx.actorSystem()

            override val logger: ILogger = ctx.jLogger()
            override val alarmService: IAlarmService = ctx.alarmService()
            override val eventService: IEventService = ctx.eventService()
            override val eventPublisher: IEventPublisher by lazy { eventService.defaultPublisher() }
            override val eventSubscriber: IEventSubscriber by lazy { eventService.defaultSubscriber() }
            override val locationService: ILocationService by lazy { JHttpLocationServiceFactory.makeLocalClient(actorSystem) }
            override val configService: IConfigClientService by lazy { JConfigClientFactory.clientApi(actorSystem, locationService) }
            override val timeService: TimeServiceScheduler by lazy { TimeServiceSchedulerFactory(actorSystem.scheduler()).make(strandEc.ec()) }
            override val databaseServiceFactory: DatabaseServiceFactory by lazy { DatabaseServiceFactory(actorSystem) }
        }
    }

    val logger: ILogger
    val alarmService: IAlarmService
    val eventService: IEventService
    val eventPublisher: IEventPublisher
    val eventSubscriber: IEventSubscriber
    val locationService: ILocationService
    val configService: IConfigClientService
    val timeService: TimeServiceScheduler
    val databaseServiceFactory: DatabaseServiceFactory
}
