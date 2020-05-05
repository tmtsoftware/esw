package esw.sm.api.actor.client

import java.net.URI

import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpLocation}
import csw.prefix.models.Prefix
import esw.sm.api.BaseTestSuite
import esw.sm.api.SequenceManagerState.Idle
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

class SequenceManagerImplTest extends ScalaTestWithActorTestKit with BaseTestSuite {
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
      case SequenceManagerMsg.CleanupDone(_)                   =>
      case SequenceManagerMsg.ConfigurationDone(_)             =>
    }
    Behaviors.same
  }

  private val smRef           = spawn(mockedBehavior)
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
