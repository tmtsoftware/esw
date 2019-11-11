package esw.gateway.server

import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import csw.alarm.api.scaladsl.AlarmAdminService
import csw.command.api.scaladsl.CommandService
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.logging.api.scaladsl.Logger
import esw.gateway.impl.LoggerCache
import esw.gateway.server.utils.CommandServiceFactory
import esw.http.core.wiring.CswWiring
import org.mockito.ArgumentMatchers.any
import org.mockito.MockitoSugar._

import scala.concurrent.duration.FiniteDuration

class CswWiringMocks() {

  val cswWiring: CswWiring = mock[CswWiring]
//  val actorRuntime: ActorRuntime = new ActorRuntime(system)
  val logger: Logger           = mock[Logger]
  val loggerCache: LoggerCache = mock[LoggerCache]
  when(loggerCache.get(any[String])).thenReturn(logger)

  //command service mocks
  val commandServiceFactory: CommandServiceFactory = mock[CommandServiceFactory]
  val commandService: CommandService               = mock[CommandService]

  //alarm service mocks
  val alarmService: AlarmAdminService = mock[AlarmAdminService]

  //event service mocks
  val eventService: EventService               = mock[EventService]
  val eventSubscriberUtil: EventSubscriberUtil = mock[EventSubscriberUtil]
  val eventPublisher: EventPublisher           = mock[EventPublisher]
  val eventSubscriber: EventSubscriber         = mock[EventSubscriber]

  when(cswWiring.eventSubscriberUtil).thenReturn(eventSubscriberUtil)
  when(cswWiring.eventService).thenReturn(eventService)
  when(cswWiring.alarmService).thenReturn(alarmService)
  when(eventService.defaultPublisher).thenReturn(eventPublisher)
  when(eventService.defaultSubscriber).thenReturn(eventSubscriber)
}

class RateLimiterStub[A](delay: FiniteDuration) extends GraphStage[FlowShape[A, A]] {
  final val in    = Inlet.create[A]("DroppingThrottle.in")
  final val out   = Outlet.create[A]("DroppingThrottle.out")
  final val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush(): Unit = {
        push(out, grab(in))
      }
    })

    setHandler(out, new OutHandler {
      override def onPull(): Unit = pull(in)
    })
  }
}
