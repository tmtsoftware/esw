package agent

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorRef, ActorSystem, Props, Scheduler, SpawnProtocol}
import akka.util.Timeout
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Subsystem

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.DurationInt

object Main extends App {

  lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "esw-system")

  implicit val timeout: Timeout     = Timeout(10.seconds)
  implicit val scheduler: Scheduler = actorSystem.scheduler
  val d: Future[ActorRef[AgentMsg]] = actorSystem ? (Spawn(AgentActor.behavior, "agent-actor", Props.empty, _))

  Await.result(d, 5.seconds) ! SpawnSequenceComponent(Subsystem.ESW, "primary")
}
