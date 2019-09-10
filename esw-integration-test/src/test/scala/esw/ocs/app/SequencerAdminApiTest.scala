package esw.ocs.app

import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType, HttpLocation}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.Step
import esw.ocs.api.request.SequencerAdminPostRequest
import esw.ocs.api.responses.{LoadSequenceResponse, Ok}
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.SequencerServer
import esw.ocs.impl.messages.SequencerMessages.{EswSequencerMessage, LoadSequence}
import mscoket.impl.post.PostClient

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerAdminApiTest extends ScalaTestFrameworkTestKit with BaseTestSuite with SequencerAdminHttpCodecs {
  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem
  private implicit val scheduler: Scheduler            = actorSystem.scheduler

  private var sequencerServer: SequencerServer            = _
  private var sequencerRef: ActorRef[EswSequencerMessage] = _
  private val locationService: LocationService            = HttpLocationServiceFactory.makeLocalClient
  private val sequencerId                                 = "testSequencerId1"
  private val observingMode                               = "testObservingMode1"
  private implicit val timeoutDuration: Timeout           = Timeout(10.seconds)

  override def beforeAll(): Unit = {
    super.beforeAll()
    val wiring = new SequencerWiring(sequencerId, observingMode, None)
    sequencerServer = wiring.sequencerServer
    sequencerServer.start()
    sequencerRef = wiring.sequencerRef
  }

  override protected def afterAll(): Unit = {
    sequencerServer.shutDown().futureValue
    super.afterAll()
  }

  "Sequencer" must {
    "start the sequencer and handle the http requests | ESW-222" in {
      val componentId                = ComponentId(s"$sequencerId@$observingMode@http", ComponentType.Service)
      val httpLocation: HttpLocation = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get

      val postClient: PostClient[SequencerAdminPostRequest] =
        new PostClient[SequencerAdminPostRequest](httpLocation.uri.toURL.toString + "post")

      val sequencerAdminClient = new SequencerAdminClient(postClient)

      sequencerAdminClient.isAvailable.futureValue should ===(true)

      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)

      val loadResponse: Future[LoadSequenceResponse] = sequencerRef ? (LoadSequence(sequence, _))

      sequencerAdminClient.getSequence.futureValue.get.steps should ===(List(Step(command1)))

      loadResponse.futureValue should ===(Ok)

//      val command2                                   = Setup(Prefix("esw.test"), CommandName("command-2"), None)
//      sequencerAdminClient.add(List(command2)).futureValue should ===(Ok)
//      sequencerAdminClient.getSequence.futureValue.get.steps should ===(List(Step(command1), Step(command2)))
    }
  }

}
