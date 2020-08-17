package esw.ocs.api.actor.client

import java.net.URI

import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, ScriptResponseOrUnhandled}
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import scala.util.Random

class SequenceComponentImplTest extends ActorTestSuit {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SequenceComponentImplTest")

  private val askProxyTestKit = new AskProxyTestKit[SequenceComponentMsg, SequenceComponentImpl] {
    override def make(actorRef: ActorRef[SequenceComponentMsg]): SequenceComponentImpl = {
      val location =
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(ESW, "sequence_component"), SequenceComponent)),
          actorRef.toURI,
          Metadata.empty
        )
      new SequenceComponentImpl(location)
    }
  }

  import askProxyTestKit._

  private def randomString5 = Random.nextString(5)

  private val obsMode   = ObsMode(randomString5)
  private val subsystem = randomSubsystem

  "LoadScript | ESW-103" in {
    val loadScriptResponse = mock[ScriptResponseOrUnhandled]
    withBehavior {
      case LoadScript(`subsystem`, `obsMode`, replyTo) => replyTo ! loadScriptResponse
    } check { sc =>
      sc.loadScript(subsystem, obsMode).futureValue should ===(loadScriptResponse)
    }
  }

  "Restart | ESW-141" in {
    val restartResponse = mock[ScriptResponseOrUnhandled]
    withBehavior {
      case RestartScript(replyTo) => replyTo ! restartResponse
    } check { sc =>
      sc.restartScript().futureValue should ===(restartResponse)
    }
  }

  "GetStatus | ESW-103" in {
    val akkaLocation =
      AkkaLocation(
        AkkaConnection(ComponentId(Prefix(ESW, "sequence_component"), SequenceComponent)),
        new URI("uri"),
        Metadata.empty
      )
    val getStatusResponse = GetStatusResponse(Some(akkaLocation))
    withBehavior {
      case GetStatus(replyTo) => replyTo ! getStatusResponse
    } check { sc =>
      sc.status.futureValue should ===(getStatusResponse)
    }
  }

  "UnloadScript | ESW-103" in {
    withBehavior {
      case UnloadScript(replyTo) => replyTo ! Ok
    } check { sc =>
      sc.unloadScript().futureValue should ===(Ok)
    }
  }

  "Shutdown | ESW-329" in {
    withBehavior {
      case Shutdown(replyTo) => replyTo ! Ok
    } check { sc =>
      sc.shutdown().futureValue should ===(Ok)
    }
  }
}
