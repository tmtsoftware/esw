package esw.ocs.api.actor.client

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.AskPattern._
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.commons.Timeouts
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.{GetStatus, LoadScript, Restart, Shutdown, UnloadScript}
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponseOrUnhandled, OkOrUnhandled, ScriptResponseOrUnhandled}

import scala.concurrent.Future

class SequenceComponentImpl(sequenceComponentLocation: AkkaLocation)(implicit
    actorSystem: ActorSystem[_]
) extends SequenceComponentApi {

  implicit val timeout: Timeout = Timeouts.AskTimeout

  private val sequenceComponentRef = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]

  override def loadScript(subsystem: Subsystem, observingMode: String): Future[ScriptResponseOrUnhandled] =
    sequenceComponentRef ? (LoadScript(subsystem, observingMode, _))

  override def restart(): Future[ScriptResponseOrUnhandled] = sequenceComponentRef ? Restart

  override def status: Future[GetStatusResponseOrUnhandled] = sequenceComponentRef ? GetStatus

  override def unloadScript(): Future[OkOrUnhandled] = sequenceComponentRef ? UnloadScript

  override def shutdown(): Future[OkOrUnhandled] = sequenceComponentRef ? Shutdown
}
