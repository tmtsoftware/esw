package esw.sm.api.actor.client

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation}
import csw.prefix.models.Prefix
import esw.sm.api.SequenceManagerState.Idle
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}
import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.DurationInt

class SequenceManagerImplTest extends AnyWordSpecLike with TypeCheckedTripleEquals with ScalaFutures with Matchers {
  private final implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SmAkkaSerializerTest")
  private implicit val timeout: Timeout                                 = 10.seconds
  private val masterSequencerLocation =
    HttpLocation(HttpConnection(ComponentId(Prefix("esw.primary"), ComponentType.Sequencer)), URI.create("uri"))
  private val configureResponse          = ConfigureResponse.Success(masterSequencerLocation)
  private val cleanupResponse            = CleanupResponse.Success
  private val getRunningObsModesResponse = GetRunningObsModesResponse.Success(Set("IRIS_Darknight", "WFOS_cal"))

  private val mockedBehavior: Behaviors.Receive[SequenceManagerMsg] = Behaviors.receiveMessage[SequenceManagerMsg] { msg =>
    msg match {
      case SequenceManagerMsg.Configure(_, replyTo)            => replyTo ! configureResponse
      case SequenceManagerMsg.Cleanup(_, replyTo)              => replyTo ! cleanupResponse
      case SequenceManagerMsg.GetRunningObsModes(replyTo)      => replyTo ! getRunningObsModesResponse
      case SequenceManagerMsg.GetSequenceManagerState(replyTo) => replyTo ! Idle
      case SequenceManagerMsg.CleanupResponseInternal(_)       =>
      case SequenceManagerMsg.ConfigurationResponseInternal(_) =>
    }
    Behaviors.same
  }

  private val smRef           = system.systemActorOf(mockedBehavior, "sm")
  private val sequenceManager = new SequenceManagerImpl(smRef)

  "SequenceManagerImpl" must {
    "configure" in {
      sequenceManager.configure("IRIS_darknight").futureValue shouldBe configureResponse
    }

    "cleanup" in {
      sequenceManager.cleanup("IRIS_darknight").futureValue shouldBe cleanupResponse
    }

    "getRunningObsModes" in {
      sequenceManager.getRunningObsModes.futureValue shouldBe getRunningObsModesResponse
    }
  }

}
