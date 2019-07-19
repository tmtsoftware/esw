package esw.ocs.internal

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

import scala.concurrent.duration.DurationInt

class SequenceComponentWiringTest extends BaseTestSuite {
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

  "SequenceComponent" must {
    "start SequenceComponent | ESW-103" in {
      val seqComName = "testSequencerComponent"
      val wiring     = new SequenceComponentWiring(seqComName)
      wiring.start()

      val connection        = AkkaConnection(ComponentId(seqComName, ComponentType.Service))
      val sequencerLocation = testLocationService.resolve(connection, 5.seconds).futureValue.get

      sequencerLocation.connection shouldBe connection
    }
  }

}
