package esw.ocs.api.actor.client

import akka.Done
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.{GetStatus, LoadScript, Restart, Shutdown, UnloadScript}
import esw.ocs.api.protocol.{GetStatusResponse, ScriptResponse}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequenceComponentImpl(sequenceComponentLocation: AkkaLocation)(implicit
    actorSystem: ActorSystem[_]
) extends SequenceComponentApi {

  implicit val timeout: Timeout = 5.seconds

  private val sequenceComponentRef = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]

  override def loadScript(subsystem: Subsystem, observingMode: String): Future[ScriptResponse] =
    sequenceComponentRef ? (LoadScript(subsystem, observingMode, _))

  override def restart(): Future[ScriptResponse] = sequenceComponentRef ? Restart

  override def status: Future[GetStatusResponse] = sequenceComponentRef ? GetStatus

  override def unloadScript(): Future[Done] = sequenceComponentRef ? UnloadScript

  override def shutdown(): Future[Done] = sequenceComponentRef ? Shutdown
}
