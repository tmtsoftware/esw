package esw.ocs.impl

import java.net.URI

import akka.Done
import akka.actor.testkit.typed.scaladsl.ScalaTestWithActorTestKit
import akka.actor.typed.scaladsl.Behaviors
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{AkkaLocation, ComponentId, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.protocol.{GetStatusResponse, ScriptError, ScriptResponse}
import esw.ocs.impl.messages.SequenceComponentMsg
import esw.ocs.impl.messages.SequenceComponentMsg._

import scala.concurrent.ExecutionContext

class SequenceComponentImplTest extends ScalaTestWithActorTestKit with BaseTestSuite {
  private val location =
    AkkaLocation(AkkaConnection(ComponentId(Prefix("esw.test"), ComponentType.Sequencer)), new URI("uri"))
  private val loadScriptResponse    = ScriptResponse(Right(location))
  private val restartResponse       = ScriptResponse(Left(ScriptError("Restart error")))
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

  private val sequenceComponentClient = new SequenceComponentImpl(sequenceComponent)

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
