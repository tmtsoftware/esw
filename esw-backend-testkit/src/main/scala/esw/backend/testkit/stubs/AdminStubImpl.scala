/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.backend.testkit.stubs

import akka.Done
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.location.api.models.ComponentId
import csw.logging.models.Level._
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.Prefix
import esw.gateway.api.AdminApi

import scala.concurrent.Future

class AdminStubImpl extends AdminApi {

  private val containerLifecycleState: ContainerLifecycleState = ContainerLifecycleState.Idle

  private val componentLifecycleState: SupervisorLifecycleState = SupervisorLifecycleState.Idle

  override def getLogMetadata(componentId: ComponentId): Future[LogMetadata] =
    Future.successful(LogMetadata(INFO, DEBUG, INFO, ERROR))

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Done] = Future.successful(Done)

  override def shutdown(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def restart(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def goOffline(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def goOnline(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def getContainerLifecycleState(prefix: Prefix): Future[ContainerLifecycleState] =
    Future.successful(containerLifecycleState)

  override def getComponentLifecycleState(componentId: ComponentId): Future[SupervisorLifecycleState] =
    Future.successful(componentLifecycleState)
}
