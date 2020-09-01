package esw.ocs.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.constants.Timeouts
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.{GetStatus, LoadScript, RestartScript, Shutdown, UnloadScript}
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, ScriptResponseOrUnhandled}

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, FiniteDuration}

class SequenceComponentImpl(sequenceComponentLocation: AkkaLocation)(implicit
    actorSystem: ActorSystem[_]
) extends SequenceComponentApi {

  private val sequenceComponentRef = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]

  override def loadScript(subsystem: Subsystem, obsMode: ObsMode): Future[ScriptResponseOrUnhandled] =
    (sequenceComponentRef ? { x: ActorRef[ScriptResponseOrUnhandled] => LoadScript(subsystem, obsMode, x) })(
      Timeouts.LoadScriptTimeout,
      actorSystem.scheduler
    )

  override def restartScript(): Future[ScriptResponseOrUnhandled] =
    (sequenceComponentRef ? RestartScript)(Timeouts.RestartScriptTimeout, actorSystem.scheduler)

  override def status: Future[GetStatusResponse] =
    (sequenceComponentRef ? GetStatus)(Timeouts.StatusTimeout, actorSystem.scheduler)

  override def unloadScript(): Future[Ok.type] =
    (sequenceComponentRef ? UnloadScript)(Timeouts.UnloadScriptTimeout, actorSystem.scheduler)

  override def shutdown(): Future[Ok.type] =
    (sequenceComponentRef ? Shutdown)(Timeouts.ShutdownTimeout, actorSystem.scheduler)
}
