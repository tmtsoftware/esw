package agent

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed._
import akka.util.Timeout
import csw.location.client.ActorSystemFactory
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}

// todo: this module should not depend on location-server (which is an app), extract http-wiring in another module and depend on that
// todo: convert to case-app
object Main extends App {

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "esw-system")

  implicit val timeout: Timeout = Timeout(10.seconds)

  implicit val scheduler: Scheduler = actorSystem.scheduler

  val d: Future[ActorRef[AgentCommand]] = actorSystem ? (Spawn(AgentActor.behavior, "agent-actor", Props.empty, _))

  // todo: Register self to location server
  // todo: merge location-agent

  Await.result(d, 5.seconds) ! SpawnSequenceComponent(Prefix(Subsystem.ESW, "primary"))
}
