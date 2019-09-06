package esw

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.typed.scaladsl.ActorMaterializer
import akka.util.Timeout
import csw.command.client.SequencerCommandServiceFactory
import csw.command.client.internal.SequencerCommandServiceImpl
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.highlevel.dsl.LocationServiceDsl
import esw.ocs.client.messages.SequencerMessages.{EswSequencerMessage, Shutdown}

import scala.concurrent.Await
import scala.concurrent.duration._

object TestClient extends App {

  implicit val system: ActorSystem[SpawnProtocol] = ActorSystemFactory.remote(SpawnProtocol.behavior)
  implicit val timeout: Timeout                   = Timeout(1.minute)
  implicit val mat: ActorMaterializer             = ActorMaterializer()
  val _locationService                            = HttpLocationServiceFactory.makeLocalClient
  import system.executionContext

  implicit val sched: Scheduler = system.scheduler

  val location = Await.result(new LocationServiceDsl {
    override private[esw] def locationService = _locationService
  }.resolveSequencer("iris", "darknight"), 5.seconds)

  private val cmd1 = Setup(Prefix("esw.a.a"), CommandName("command-1"), None)
  private val cmd2 = Setup(Prefix("esw.a.a"), CommandName("command-2"), None)
  private val cmd3 = Setup(Prefix("esw.a.a"), CommandName("command-3"), None)

  private val factory: SequencerCommandServiceImpl = SequencerCommandServiceFactory.make(location)

  factory.submit(Sequence(cmd1, cmd2, cmd3)).onComplete { _ =>
    Thread.sleep(2000)
    Await.result(location.uri.toActorRef.unsafeUpcast[EswSequencerMessage] ? Shutdown, 10.seconds)
    system.terminate()
  }

}
