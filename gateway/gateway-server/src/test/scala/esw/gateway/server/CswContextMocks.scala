package esw.gateway.server

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.command.api.scaladsl.CommandService
import csw.event.api.scaladsl.{EventPublisher, EventService, EventSubscriber}
import esw.template.http.server.commons.ActorRuntime
import esw.template.http.server.{ComponentFactory, CswContext}
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

trait CswContextMocks extends MockitoSugar {

  private val system: ActorSystem[SpawnProtocol] = ActorSystem(SpawnProtocol.behavior, "test-system")

  val componentFactory: ComponentFactory = mock[ComponentFactory]
  val commandService: CommandService     = mock[CommandService]
  val eventService: EventService         = mock[EventService]
  val cswCtx: CswContext                 = mock[CswContext]
  val actorRuntime: ActorRuntime         = new ActorRuntime(system)
  val eventPublisher: EventPublisher     = mock[EventPublisher]
  val eventSubscriber: EventSubscriber   = mock[EventSubscriber]

  when(eventService.defaultPublisher).thenReturn(eventPublisher)
  when(eventService.defaultSubscriber).thenReturn(eventSubscriber)
  when(cswCtx.actorSystem).thenReturn(system)
  when(cswCtx.actorRuntime).thenReturn(actorRuntime)
  when(cswCtx.eventService).thenReturn(eventService)
  when(cswCtx.componentFactory).thenReturn(componentFactory)
}
