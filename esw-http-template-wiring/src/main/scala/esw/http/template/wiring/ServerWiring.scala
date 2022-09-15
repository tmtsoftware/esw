/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.http.template.wiring

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.typesafe.config.Config
import csw.aas.http.SecurityDirectives
import csw.alarm.api.scaladsl.AlarmService
import csw.alarm.client.AlarmServiceFactory
import csw.config.api.scaladsl.ConfigClientService
import csw.config.client.scaladsl.ConfigClientFactory
import csw.event.api.scaladsl.EventService
import csw.event.client.EventServiceFactory
import csw.event.client.models.EventStores.RedisStore
import csw.location.api.models.{ComponentType, Metadata}
import csw.location.api.scaladsl.{LocationService, RegistrationResult}
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.LoggerFactory
import csw.time.scheduler.TimeServiceSchedulerFactory
import csw.time.scheduler.api.TimeServiceScheduler
import esw.http.core.commons.ServiceLogger
import esw.http.core.wiring.{ActorRuntime, HttpService, Settings}
import io.lettuce.core.RedisClient

import scala.concurrent.Future
import scala.util.chaining.scalaUtilChainingOps

trait ServerWiring {
  def port: Option[Int]
  def actorSystemName: String
  def routes: Route

  private lazy val actorSystem: ActorSystem[SpawnProtocol.Command] =
    ActorSystemFactory.remote(SpawnProtocol(), actorSystemName)

  private lazy val config: Config = actorSystem.settings.config
  final lazy val settings         = new Settings(port, None, config, ComponentType.Service)
  final lazy val actorRuntime     = new ActorRuntime(actorSystem)
  import actorRuntime.{ec, typedSystem}

  private lazy val locationService: LocationService         = HttpLocationServiceFactory.makeLocalClient(actorSystem)
  private lazy val configClientService: ConfigClientService = ConfigClientFactory.clientApi(actorSystem, locationService)
  private lazy val loggerFactory: LoggerFactory             = new ServiceLogger(settings.prefix)

  private lazy val redisClient: RedisClient                 = RedisClient.create().tap(shutdownRedisOnTermination)
  private lazy val eventServiceFactory: EventServiceFactory = new EventServiceFactory(RedisStore(redisClient))
  private lazy val eventService: EventService               = eventServiceFactory.make(locationService)
  private lazy val alarmServiceFactory: AlarmServiceFactory = new AlarmServiceFactory(redisClient)
  private lazy val alarmService: AlarmService               = alarmServiceFactory.makeClientApi(locationService)

  private lazy val timeServiceSchedulerFactory                = new TimeServiceSchedulerFactory()(actorSystem.scheduler)
  private lazy val timeServiceScheduler: TimeServiceScheduler = timeServiceSchedulerFactory.make()

  final lazy val cswServices =
    CswServices(locationService, eventService, alarmService, timeServiceScheduler, loggerFactory, configClientService)
  final lazy val jCswServices = cswServices.asJava

  final lazy val logger: Logger = cswServices.loggerFactory.getLogger

  final lazy val securityDirectives: SecurityDirectives = SecurityDirectives(config, cswServices.locationService)
  private lazy val service = new HttpService(logger, cswServices.locationService, routes, settings, actorRuntime)

  def start(metadata: Metadata): Future[(Http.ServerBinding, RegistrationResult)] =
    service.startAndRegisterServer(metadata)

  def stop(): Future[Done] = actorRuntime.shutdown(UnknownReason)

  private def shutdownRedisOnTermination(client: RedisClient)(implicit actorSystem: ActorSystem[_]): Unit = {
    CoordinatedShutdown(actorSystem).addTask(
      CoordinatedShutdown.PhaseBeforeServiceUnbind,
      "redis-client-shutdown"
    )(() => Future { client.shutdown(); Done })
  }
}
