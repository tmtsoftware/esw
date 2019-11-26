package esw

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.stream.Materializer
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import esw.ocs.dsl.sequence_manager.LocationServiceUtil
import esw.ocs.impl.SequencerAdminImpl
import esw.ocs.impl.messages.SequencerMessages.{EswSequencerMessage, Shutdown}

import scala.concurrent.Await
import scala.concurrent.duration._

object TestClient extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  implicit val timeout: Timeout                           = Timeout(1.minute)
  implicit val mat: Materializer                          = Materializer(system)
  val _locationService                                    = HttpLocationServiceFactory.makeLocalClient
  import system.executionContext

  implicit val sched: Scheduler = system.scheduler

  val location = Await.result(
    new LocationServiceUtil(_locationService)
      .resolveSequencer("iris", "darknight"),
    5.seconds
  )

  private val cmd1 = Setup(Prefix("esw.a.a"), CommandName("command-1"), None)
  private val cmd2 = Setup(Prefix("esw.a.a"), CommandName("command-2"), None)
  private val cmd3 = Setup(Prefix("esw.a.a"), CommandName("command-3"), None)

  import csw.command.client.extensions.AkkaLocationExt._
  private val sequencerAdmin = new SequencerAdminImpl(location.sequencerRef)

  sequencerAdmin.submitAndWait(Sequence(cmd1, cmd2, cmd3)).onComplete { _ =>
    Thread.sleep(2000)
    Await.result(location.uri.toActorRef.unsafeUpcast[EswSequencerMessage] ? Shutdown, 10.seconds)
    system.terminate()
  }

}
