package esw.ocs.api.actor.client

import org.apache.pekko.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.PekkoConnection
import csw.location.api.models.{PekkoLocation, ComponentId, Metadata}
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.*
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, ScriptResponseOrUnhandled}
import esw.testcommons.{ActorTestSuit, AskProxyTestKit}

import java.net.URI

class SequenceComponentImplTest extends ActorTestSuit {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SequenceComponentImplTest")

  private val askProxyTestKit = new AskProxyTestKit[SequenceComponentMsg, SequenceComponentImpl] {
    override def make(actorRef: ActorRef[SequenceComponentMsg]): SequenceComponentImpl = {
      val location =
        PekkoLocation(
          PekkoConnection(ComponentId(Prefix(ESW, "sequence_component"), SequenceComponent)),
          actorRef.toURI,
          Metadata.empty
        )
      new SequenceComponentImpl(location)
    }
  }

  import askProxyTestKit.*

  private val obsMode   = ObsMode(randomString5)
  private val subsystem = randomSubsystem
  private val variation = Some(Variation(randomString5))

  "LoadScript | ESW-103, ESW-362, ESW-561" in {
    val loadScriptResponse = mock[ScriptResponseOrUnhandled]
    withBehavior { case LoadScript(replyTo, `subsystem`, `obsMode`, `variation`) =>
      replyTo ! loadScriptResponse
    } check { sc =>
      sc.loadScript(subsystem, obsMode, variation).futureValue should ===(loadScriptResponse)
    }
  }

  "Restart | ESW-141, ESW-362" in {
    val restartResponse = mock[ScriptResponseOrUnhandled]
    withBehavior { case RestartScript(replyTo) =>
      replyTo ! restartResponse
    } check { sc =>
      sc.restartScript().futureValue should ===(restartResponse)
    }
  }

  "GetStatus | ESW-103, ESW-362" in {
    val pekkoLocation =
      PekkoLocation(
        PekkoConnection(ComponentId(Prefix(subsystem, "sequence_component"), SequenceComponent)),
        new URI("uri"),
        Metadata.empty
      )
    val getStatusResponse = GetStatusResponse(Some(pekkoLocation))
    withBehavior { case GetStatus(replyTo) =>
      replyTo ! getStatusResponse
    } check { sc =>
      sc.status.futureValue should ===(getStatusResponse)
    }
  }

  "UnloadScript | ESW-103, ESW-362" in {
    withBehavior { case UnloadScript(replyTo) =>
      replyTo ! Ok
    } check { sc =>
      sc.unloadScript().futureValue should ===(Ok)
    }
  }

  "Shutdown | ESW-329, ESW-362" in {
    withBehavior { case Shutdown(replyTo) =>
      replyTo ! Ok
    } check { sc =>
      sc.shutdown().futureValue should ===(Ok)
    }
  }
}
