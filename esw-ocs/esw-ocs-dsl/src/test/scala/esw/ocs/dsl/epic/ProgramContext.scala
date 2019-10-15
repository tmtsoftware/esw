package esw.ocs.dsl.epic

import akka.Done
import akka.actor.typed.ActorSystem
import akka.stream.Materializer
import akka.stream.typed.scaladsl.ActorMaterializer
import esw.ocs.dsl.script.StrandEc
import esw.ocs.dsl.epic.internal.Machine
import esw.ocs.dsl.epic.internal.event.MockEventService

import scala.concurrent.{ExecutionContext, Future}

trait ProgramContext {
  implicit def strandEc: StrandEc
  implicit def ec: ExecutionContext
  implicit def mat: Materializer
  implicit def eventService: MockEventService
}

trait Refreshable {
  def refresh(source: String): Future[Done]
}

abstract class Program(actorSystem: ActorSystem[_]) extends Refreshable { outer =>
  private var machines: List[Machine[_]] = List.empty

  implicit object Context extends ProgramContext {
    implicit lazy val strandEc: StrandEc             = StrandEc()
    implicit lazy val ec: ExecutionContext           = strandEc.ec
    implicit lazy val mat: Materializer              = ActorMaterializer()(actorSystem)
    implicit lazy val eventService: MockEventService = new MockEventService()(actorSystem)
  }

  implicit lazy val Refreshable: Refreshable = outer
  import Context._

  def refresh(source: String): Future[Done] = Future.traverse(machines)(_.refresh(source)).map(_ => Done)

  def setup(machine: Machine[_]): Unit = {
    machines ::= machine
  }
}
