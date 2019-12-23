package agent

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.SpawnProtocol.Spawn
import akka.actor.typed._
import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.util.Timeout
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.impl.internal.{ServerWiring, Settings}
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaRegistration, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.Try

// todo: this module should not depend on location-server (which is an app), extract http-wiring in another module and depend on that
// todo: convert to case-app
// todo: Add support for default actions e.g. redis
// todo: merge location-agent
object Main extends App {

  //todo: remove creation of this actor system, reuse cluster system
  implicit lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "esw-system")
  implicit val timeout: Timeout                                     = Timeout(10.seconds)
  implicit val scheduler: Scheduler                                 = actorSystem.scheduler
  import actorSystem.executionContext

  private val coordinatedShutdown = CoordinatedShutdown(actorSystem.toClassic)
  val agentConnection             = AkkaConnection(ComponentId(Prefix(Subsystem.ESW, "Agent"), ComponentType.Machine))

  val wiring          = new ServerWiring(Settings("agent"))
  val locationBinding = Await.result(wiring.locationHttpService.start(), timeout.duration)
  coordinatedShutdown.addTask(CoordinatedShutdown.PhaseServiceUnbind, "unbind-services") { () =>
    locationService
      .unregister(agentConnection)
      .transform(_ => Try(locationBinding.terminate(timeout.duration).map(_ => Done)))
      .flatten
  }

  // spawn agent actor and register to location server
  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val actor                            = new AgentActor(locationService)
  val agentRef: ActorRef[AgentCommand] =
    Await.result(actorSystem ? (Spawn(actor.behavior, "agent-actor", Props.empty, _)), timeout.duration)

  val regResult = Await.result(locationService.register(AkkaRegistration(agentConnection, agentRef.toURI)), timeout.duration)

  // Test messages
  val response: Future[Response] = agentRef ? SpawnSequenceComponent(Prefix(Subsystem.ESW, "primary"))
  println("Response=" + Await.result(response, 10.seconds))
}
