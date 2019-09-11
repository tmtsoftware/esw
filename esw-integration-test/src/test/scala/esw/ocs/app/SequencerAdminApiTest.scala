package esw.ocs.app

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.HttpConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.client.SequencerAdminClient
import esw.ocs.api.codecs.SequencerAdminHttpCodecs
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{LoadSequenceResponse, Ok, SequencerAdminPostRequest, SequencerAdminWebsocketRequest}
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.impl.internal.SequencerServer
import mscoket.impl.post.PostClient
import mscoket.impl.ws.WebsocketClient

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerAdminApiTest extends ScalaTestFrameworkTestKit with BaseTestSuite with SequencerAdminHttpCodecs {
  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  private var sequencerServer: SequencerServer = _
  private val locationService: LocationService = HttpLocationServiceFactory.makeLocalClient
  private val sequencerId                      = "testSequencerId1"
  private val observingMode                    = "testObservingMode1"

  override def beforeAll(): Unit = {
    super.beforeAll()
    val wiring = new SequencerWiring(sequencerId, observingMode, None)
    sequencerServer = wiring.sequencerServer
    sequencerServer.start()
  }

  override protected def afterAll(): Unit = {
    sequencerServer.shutDown().futureValue
    super.afterAll()
  }

  "Sequencer" must {
    "start the sequencer and handle the http requests | ESW-222" in {
      val componentId          = ComponentId(s"$sequencerId@$observingMode@http", ComponentType.Service)
      val url                  = locationService.resolve(HttpConnection(componentId), 5.seconds).futureValue.get.uri.toString
      val postClient           = new PostClient[SequencerAdminPostRequest](url + "post")
      val websocketClient      = new WebsocketClient[SequencerAdminWebsocketRequest](url + "websocket")
      val sequencerAdminClient = new SequencerAdminClient(postClient, websocketClient)

      sequencerAdminClient.isAvailable.futureValue should ===(true)

      val command1 = Setup(Prefix("esw.test"), CommandName("command-1"), None)
      val sequence = Sequence(command1)

      val loadResponse: Future[LoadSequenceResponse] = sequencerAdminClient.loadSequence(sequence)

      loadResponse.futureValue should ===(Ok)

      sequencerAdminClient.getSequence.futureValue.get.steps should ===(List(Step(command1)))
//      val command2                                   = Setup(Prefix("esw.test"), CommandName("command-2"), None)
//      sequencerAdminClient.add(List(command2)).futureValue should ===(Ok)
//      sequencerAdminClient.getSequence.futureValue.get.steps should ===(List(Step(command1), Step(command2)))
    }
  }

}
