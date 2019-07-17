package esw.gateway.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.http.scaladsl.server.Route
import akka.stream.stage.{GraphStage, GraphStageLogic, InHandler, OutHandler}
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import csw.command.api.scaladsl.CommandService
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import csw.event.client.internal.commons.EventSubscriberUtil
import csw.logging.api.scaladsl.Logger
import esw.gateway.server.routes.Routes
import esw.template.http.server.commons.RouteHandlers
import esw.template.http.server.csw.utils.{ComponentFactory, CswContext}
import esw.template.http.server.wiring.ActorRuntime
import org.mockito.MockitoSugar

import scala.concurrent.duration.FiniteDuration

class CswContextMocks(system: ActorSystem[SpawnProtocol]) {

  import MockitoSugar._

  val cswCtx: CswContext         = mock[CswContext]
  val actorRuntime: ActorRuntime = new ActorRuntime(system)
  val logger: Logger             = mock[Logger]

  //command service mocks
  val componentFactory: ComponentFactory = mock[ComponentFactory]
  val commandService: CommandService     = mock[CommandService]

  //event service mocks
  val eventService: EventService               = mock[EventService]
  val eventSubscriberUtil: EventSubscriberUtil = mock[EventSubscriberUtil]
  val eventPublisher: EventPublisher           = mock[EventPublisher]
  val eventSubscriber: EventSubscriber         = mock[EventSubscriber]
  val handlers: RouteHandlers                  = new RouteHandlers(logger)

  val route: Route = new Routes(cswCtx).route

  when(cswCtx.logger).thenReturn(logger)
  when(cswCtx.routeHandlers).thenReturn(handlers)

  when(cswCtx.componentFactory).thenReturn(componentFactory)

  when(cswCtx.eventSubscriberUtil).thenReturn(eventSubscriberUtil)
  when(cswCtx.eventService).thenReturn(eventService)
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
