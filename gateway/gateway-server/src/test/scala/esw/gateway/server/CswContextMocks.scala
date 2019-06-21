package esw.gateway.server

import akka.util.Timeout
import csw.command.api.scaladsl.CommandService
import esw.template.http.server.{ComponentFactory, CswContext}
import esw.template.http.server.commons.ActorRuntime
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.ExecutionContext.Implicits
import scala.concurrent.duration.DurationDouble

trait CswContextMocks extends MockitoSugar {
  val componentFactory: ComponentFactory = mock[ComponentFactory]
  val commandService: CommandService     = mock[CommandService]
  val cswCtx: CswContext                 = mock[CswContext]
  val actorRuntime: ActorRuntime         = mock[ActorRuntime]
  implicit val timeout: Timeout          = Timeout(5.seconds)

  when(actorRuntime.ec).thenReturn(Implicits.global)
  when(actorRuntime.timeout).thenReturn(timeout)
  when(cswCtx.actorRuntime).thenReturn(actorRuntime)
  when(cswCtx.componentFactory).thenReturn(componentFactory)
}
