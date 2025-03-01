package esw.demo

import org.apache.pekko.actor.typed.scaladsl.AskPattern.Askable
import org.apache.pekko.actor.typed.{ActorSystem, Scheduler, SpawnProtocol}
import org.apache.pekko.stream.Materializer
import org.apache.pekko.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.PekkoLocation
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.IRIS
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.UnloadScript
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.concurrent.Await
import scala.concurrent.duration.*

object TestClient {

  implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol())
  implicit val timeout: Timeout                           = Timeout(1.minute)
  implicit val mat: Materializer                          = Materializer(system)
  val _locationService: LocationService                   = HttpLocationServiceFactory.makeLocalClient
  import system.executionContext

  implicit val sched: Scheduler = system.scheduler

  private val pekkoLocation: PekkoLocation = new LocationServiceUtil(_locationService)
    .findSequencer(Prefix(IRIS, "darknight"))
    .futureValue
    .toOption
    .get
  private val sequencer = SequencerApiFactory.make(pekkoLocation)

  private val cmd1 = Setup(Prefix("esw.a.a"), CommandName("command-1"), None)
  private val cmd2 = Setup(Prefix("esw.a.a"), CommandName("command-2"), None)
  private val cmd3 = Setup(Prefix("esw.a.a"), CommandName("command-3"), None)

  def main(args: Array[String]): Unit = {
    sequencer.submitAndWait(Sequence(cmd1, cmd2, cmd3)).onComplete { _ =>
      Thread.sleep(2000)
      val eventualDone =
        sequencer.getSequenceComponent.flatMap(_.uri.toActorRef.unsafeUpcast[SequenceComponentMsg] ? UnloadScript.apply)
      Await.result(eventualDone, 10.seconds)
      system.terminate()
    }
  }

}
