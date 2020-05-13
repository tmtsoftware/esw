package esw.ocs.api.actor.client

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.location.api.extensions.ActorExtension.RichActor
import csw.location.api.models.ComponentType.SequenceComponent
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg._
import esw.ocs.api.protocol.ScriptError.ScriptError
import esw.ocs.api.protocol.{GetStatusResponse, LoadScriptResponse, RestartScriptResponse}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.ExecutionContext

class SequenceComponentImplTest extends ScalaTestWithActorTestKit with AnyWordSpecLike {
  private val location =
    AkkaLocation(AkkaConnection(ComponentId(Prefix("esw.test"), ComponentType.Sequencer)), new URI("uri"))
  private val loadScriptResponse    = LoadScriptResponse(Right(location))
  private val restartResponse       = RestartScriptResponse(Left(ScriptError("script loading failed")))
  private val getStatusResponse     = GetStatusResponse(Some(location))
  implicit val ec: ExecutionContext = system.executionContext

  private val mockedBehavior: Behaviors.Receive[SequenceComponentMsg] = Behaviors.receiveMessage[SequenceComponentMsg] { msg =>
    msg match {
      case LoadScript(_, _, replyTo) => replyTo ! loadScriptResponse
      case GetStatus(replyTo)        => replyTo ! getStatusResponse
      case UnloadScript(replyTo)     => replyTo ! Done
      case Restart(replyTo)          => replyTo ! restartResponse
      case Stop                      => Behaviors.stopped
    }
    Behaviors.same
  }

  private val sequenceComponent = spawn(mockedBehavior)
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
    sequenceComponentClient.unloadScript().futureValue should ===(Done)
  }
}
