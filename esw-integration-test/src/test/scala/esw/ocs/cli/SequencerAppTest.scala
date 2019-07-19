package esw.ocs.cli

import akka.actor
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter.TypedActorSystemOps
import akka.http.scaladsl.Http
import akka.stream.typed.scaladsl.ActorMaterializer
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.model.scaladsl.Connection.AkkaConnection
import csw.location.model.scaladsl.{ComponentId, ComponentType}
import csw.testkit.LocationTestKit
import esw.ocs.BaseTestSuite
import esw.ocs.cli.SequencerAppCommand.{SequenceComponent, Sequencer}
import esw.ocs.internal.SequencerWiring

import scala.concurrent.duration.DurationInt

class SequencerAppTest extends BaseTestSuite {
  val testKit = LocationTestKit()

  implicit val system: ActorSystem[_]                = ActorSystem(Behaviors.empty, "test")
  implicit val untypedActorSystem: actor.ActorSystem = system.toUntyped
  implicit val mat: ActorMaterializer                = ActorMaterializer()
  private val testLocationService: LocationService   = HttpLocationServiceFactory.makeLocalClient

  override def beforeAll(): Unit = {
    testKit.startLocationServer()
  }

  override def afterAll(): Unit = {
    Http().shutdownAllConnectionPools().futureValue
    testKit.shutdownLocationServer()
    system.terminate()
    system.whenTerminated.futureValue
  }

  "SequenceComponent command" must {
    "start sequence component with provided name and register it with location service | ESW-103, ESW-147" in {
      val seqComName = "testSequencerComponent"
      SequencerApp.run(SequenceComponent(seqComName))

      val connection        = AkkaConnection(ComponentId(seqComName, ComponentType.Service))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.get

      sequencerLocation.connection shouldBe connection
    }
  }

  "Sequencer command" must {
    "start sequencer with provided id, mode and register it with location service | ESW-103, ESW-147" in {
      val sequencerId   = "testSequencerId1"
      val observingMode = "testObservingMode1"
      val sequencerName = s"$sequencerId@$observingMode"
      SequencerApp.run(Sequencer(sequencerId, observingMode))

      val connection        = AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.value

      sequencerLocation.connection.componentId.name shouldBe sequencerName
    }
  }

}
