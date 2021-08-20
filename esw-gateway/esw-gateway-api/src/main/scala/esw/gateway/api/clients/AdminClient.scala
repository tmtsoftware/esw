package esw.gateway.api.clients

import akka.Done
import csw.command.client.cbor.MessageCodecs
import csw.command.client.models.framework.{ContainerLifecycleState, SupervisorLifecycleState}
import csw.location.api.models.ComponentId
import csw.logging.models.codecs.LoggingCodecs
import csw.logging.models.{Level, LogMetadata}
import csw.prefix.models.Prefix
import esw.gateway.api.AdminApi
import esw.gateway.api.protocol.GatewayRequest
import esw.gateway.api.protocol.GatewayRequest._
import msocket.api.Transport
import msocket.api.codecs.BasicCodecs

import scala.concurrent.Future

/**
 * HTTP client for the Admin Service
 * @param postClient - An Transport class for HTTP calls for the Admin Service
 */
class AdminClient(postClient: Transport[GatewayRequest]) extends AdminApi with LoggingCodecs with BasicCodecs with MessageCodecs {

  override def shutdown(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](Shutdown(componentId))

  override def restart(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](Restart(componentId))

  override def goOffline(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](GoOffline(componentId))

  override def goOnline(componentId: ComponentId): Future[Done] = postClient.requestResponse[Done](GoOnline(componentId))

  override def getContainerLifecycleState(prefix: Prefix): Future[ContainerLifecycleState] =
    postClient.requestResponse[ContainerLifecycleState](GetContainerLifecycleState(prefix))

  override def getComponentLifecycleState(componentId: ComponentId): Future[SupervisorLifecycleState] =
    postClient.requestResponse[SupervisorLifecycleState](GetComponentLifecycleState(componentId))

  def getLogMetadata(componentId: ComponentId): Future[LogMetadata] =
    postClient.requestResponse[LogMetadata](GetLogMetadata(componentId))

  def setLogLevel(componentId: ComponentId, level: Level): Future[Done] =
    postClient.requestResponse[Done](SetLogLevel(componentId, level))
}
