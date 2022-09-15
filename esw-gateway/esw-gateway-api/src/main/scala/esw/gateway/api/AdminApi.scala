/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.gateway.api

import akka.Done
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.location.api.models.ComponentId
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.Prefix

import scala.concurrent.Future

trait AdminApi {

  /**
   * Shuts down the given component(HCD, Assembly or Container)
   *
   * @param componentId the [[csw.location.api.models.ComponentId]] of the component which needs to be shut down
   * @return a Done as a Future value
   */
  def shutdown(componentId: ComponentId): Future[Done]

  /**
   * Restarts the given component(HCD, Assembly or Container)
   *
   * @param componentId the [[csw.location.api.models.ComponentId]] of the component which needs to be restarted
   * @return a Done as a Future value
   */
  def restart(componentId: ComponentId): Future[Done]

  /**
   * Sets the current Lifecycle state of the given component(HCD, Assembly or Container) to Offline
   *
   * @param componentId the [[csw.location.api.models.ComponentId]] of the component which needs to be offline
   * @return a Done as a Future value
   */
  def goOffline(componentId: ComponentId): Future[Done]

  /**
   * Sets the current Lifecycle state of the given component(HCD, Assembly or Container) to Online
   *
   * @param componentId the [[csw.location.api.models.ComponentId]] of the component which needs to be online
   * @return a Done as a Future value
   */
  def goOnline(componentId: ComponentId): Future[Done]

  /**
   * Fetches the current Lifecycle state of the given container
   *
   * @param prefix the [[csw.prefix.models.Prefix]] of the container whose lifecycle state needs to be fetched
   * @return a ContainerLifecycleState as a Future value
   */
  def getContainerLifecycleState(prefix: Prefix): Future[ContainerLifecycleState]

  /**
   * Fetches the current Lifecycle state of the given Assembly or HCD
   *
   * @param componentId the [[csw.location.api.models.ComponentId]] of the component whose lifecycle state needs to be fetched
   * @return a SupervisorLifecycleState as a Future value
   */
  def getComponentLifecycleState(componentId: ComponentId): Future[SupervisorLifecycleState]

  /**
   * Fetches the LogMetadata for given component
   *
   * @param componentId   the component whose LogMetadata needs to be fetched
   * @return              a Future which completes and gives LogMetaData of the component
   */
  def getLogMetadata(componentId: ComponentId): Future[LogMetadata]

  /**
   * Updates the log level of component
   *
   * @param componentId   the component whose log level to be changed
   * @param level         represents log level to set
   */
  def setLogLevel(componentId: ComponentId, level: Level): Future[Done]
}
