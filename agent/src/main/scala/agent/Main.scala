package agent

import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.util.Timeout
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

// todo: this module should not depend on location-server (which is an app), extract http-wiring in another module and depend on that
// todo: convert to case-app
// todo: Add support for default actions e.g. redis
object Main extends App {

  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "esw-system")
  private val locationService: LocationService                      = HttpLocationServiceFactory.makeLocalClient

  implicit val timeout: Timeout = Timeout(10.seconds)

  implicit val scheduler: Scheduler = actorSystem.scheduler
  implicit val ec: ExecutionContext = actorSystem.executionContext

  val agentRefF: Future[ActorRef[AgentCommand]] = actorSystem ? (Spawn(AgentActor.behavior, "agent-actor", Props.empty, _))

  val regResultF = agentRefF.flatMap { ref =>
    locationService.register(
      AkkaRegistration(AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "Agent"), ComponentType.Machine)), ref.toURI)
    )
  }

  // fixme: This should make sure all default tasks are spawned before onFailure is triggered
  regResultF.onComplete {
    case Failure(exception) => actorSystem.terminate(); exception.printStackTrace()
    case Success(_)         =>
  }

  // todo: Register self to location server
  // todo: merge location-agent

  Await.result(agentRefF, 5.seconds) ! SpawnSequenceComponent(Prefix(Subsystem.ESW, "primary"))
}
