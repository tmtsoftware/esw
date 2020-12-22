package esw.backend.testkit.stubs

import akka.Done
import csw.location.api.models.ComponentId
import csw.logging.models.Level._
import csw.logging.models.{Level, LogMetadata}
import esw.gateway.api.AdminApi

import scala.concurrent.Future

class AdminStubImpl extends AdminApi {
  override def getLogMetadata(componentId: ComponentId): Future[LogMetadata] =
    Future.successful(LogMetadata(INFO, DEBUG, INFO, ERROR))

  override def setLogLevel(componentId: ComponentId, level: Level): Future[Done] = Future.successful(Done)

  override def shutdown(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def restart(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def goOffline(componentId: ComponentId): Future[Done] = Future.successful(Done)

  override def goOnline(componentId: ComponentId): Future[Done] = Future.successful(Done)
}
