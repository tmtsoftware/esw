package esw.gateway.api

import akka.Done
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.location.api.models.ComponentId
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.Prefix

import scala.concurrent.Future

trait AdminApi {

  def shutdown(componentId: ComponentId): Future[Done]
  def restart(componentId: ComponentId): Future[Done]
  def goOffline(componentId: ComponentId): Future[Done]
  def goOnline(componentId: ComponentId): Future[Done]

  def getContainerLifecycleState(prefix: Prefix): Future[ContainerLifecycleState]
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
