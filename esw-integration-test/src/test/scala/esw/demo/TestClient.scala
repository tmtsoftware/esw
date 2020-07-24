package esw.demo

import akka.actor.typed.scaladsl.AskPattern.Askable
import akka.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import akka.stream.Materializer
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.UnloadScript
import esw.ocs.api.models.ObsMode
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.Await
import scala.concurrent.duration._

object TestClient extends App {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  implicit val timeout: Timeout                           = Timeout(1.minute)
  implicit val mat: Materializer                          = Materializer(system)
  val _locationService                                    = HttpLocationServiceFactory.makeLocalClient
  import system.executionContext

  implicit val sched: Scheduler = system.scheduler

  private val akkaLocation: AkkaLocation = new LocationServiceUtil(_locationService)
    .findSequencer(IRIS, ObsMode("darknight"))
    .futureValue
    .toOption
    .get
  private val sequencer = SequencerApiFactory.make(akkaLocation)

  private val cmd1 = Setup(Prefix("esw.a.a"), CommandName("command-1"), None)
  private val cmd2 = Setup(Prefix("esw.a.a"), CommandName("command-2"), None)
  private val cmd3 = Setup(Prefix("esw.a.a"), CommandName("command-3"), None)

  sequencer.submitAndWait(Sequence(cmd1, cmd2, cmd3)).onComplete { _ =>
    Thread.sleep(2000)
    val eventualDone = sequencer.getSequenceComponent.flatMap(_.uri.toActorRef.unsafeUpcast[SequenceComponentMsg] ? UnloadScript)
    Await.result(eventualDone, 10.seconds)
    system.terminate()
  }

}
