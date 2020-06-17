package esw.ocs.api.actor.client

import java.net.URI

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.BaseTestSuite
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.protocol.ScriptError.LocationServiceError
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, SequencerLocation}

import scala.concurrent.ExecutionContext

class SequenceComponentImplTest extends BaseTestSuite {
  private implicit val system: ActorSystem[SpawnProtocol.Command] = ActorSystem(SpawnProtocol(), "SequenceComponentImplTest")

  private val location =
    AkkaLocation(AkkaConnection(ComponentId(Prefix("esw.test"), ComponentType.Sequencer)), new URI("uri"))
  private val loadScriptResponse    = SequencerLocation(location)
  private val restartResponse       = LocationServiceError("error")
  private val getStatusResponse     = GetStatusResponse(Some(location))
  implicit val ec: ExecutionContext = system.executionContext

  private val mockedBehavior: Behaviors.Receive[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] {
    case LoadScript(_, _, replyTo) => replyTo ! loadScriptResponse; Behaviors.same
    case GetStatus(replyTo)        => replyTo ! getStatusResponse; Behaviors.same
    case UnloadScript(replyTo)     => replyTo ! Ok; Behaviors.same
    case Restart(replyTo)          => replyTo ! restartResponse; Behaviors.same
    case Stop                      => Behaviors.stopped
    case Shutdown(replyTo)         => replyTo ! Ok; Behaviors.stopped
    case ShutdownInternal(_)       => Behaviors.unhandled
  }

  private val sequenceComponent = system.systemActorOf(mockedBehavior, "sequence_component")
  private val sequenceComponentLocation = AkkaLocation(
    AkkaConnection(ComponentId(Prefix(ESW, "primary"), SequenceComponent)),
    sequenceComponent.toURI
  )

  private val sequenceComponentClient = new SequenceComponentImpl(sequenceComponentLocation)

  "LoadScript | ESW-103" in {
    sequenceComponentClient.loadScript(Subsystem.ESW, "darknight").futureValue should ===(loadScriptResponse)
  }

  "Restart | ESW-141" in {
    sequenceComponentClient.restart().futureValue should ===(restartResponse)
  }

  "GetStatus | ESW-103" in {
    sequenceComponentClient.status.futureValue should ===(getStatusResponse)
  }

  "UnloadScript | ESW-103" in {
    sequenceComponentClient.unloadScript().futureValue should ===(Ok)
  }

  "Shutdown | ESW-329" in {
    sequenceComponentClient.shutdown().futureValue should ===(Ok)
  }
}
