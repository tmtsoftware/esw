package esw.ocs.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.{GetStatus, LoadScript, RestartScript, Shutdown, UnloadScript}
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, OkOrUnhandled, ScriptResponseOrUnhandled}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class SequenceComponentImpl(sequenceComponentLocation: AkkaLocation)(implicit
    actorSystem: ActorSystem[_]
) extends SequenceComponentApi {

  private val sequenceComponentRef = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]

  override def loadScript(subsystem: Subsystem, observingMode: ObsMode): Future[ScriptResponseOrUnhandled] =
    (sequenceComponentRef ? { x: ActorRef[ScriptResponseOrUnhandled] => LoadScript(subsystem, observingMode, x) })(
      SequenceComponentApiTimeout.LoadScriptTimeout,
      actorSystem.scheduler
    )

  override def restartScript(): Future[ScriptResponseOrUnhandled] =
    (sequenceComponentRef ? RestartScript)(SequenceComponentApiTimeout.RestartScriptTimeout, actorSystem.scheduler)

  override def status: Future[GetStatusResponse] =
    (sequenceComponentRef ? GetStatus)(SequenceComponentApiTimeout.LookupTimeout, actorSystem.scheduler)

  override def unloadScript(): Future[OkOrUnhandled] =
    (sequenceComponentRef ? UnloadScript)(SequenceComponentApiTimeout.UnloadScriptTimeout, actorSystem.scheduler)

  override def shutdown(): Future[OkOrUnhandled] =
    (sequenceComponentRef ? Shutdown)(SequenceComponentApiTimeout.ShutdownTimeout, actorSystem.scheduler)
}

object SequenceComponentApiTimeout {
  val LookupTimeout: Timeout        = 1.seconds
  val LoadScriptTimeout: Timeout    = 5.seconds
  val UnloadScriptTimeout: Timeout  = 3.seconds
  val RestartScriptTimeout: Timeout = 3.seconds
  val ShutdownTimeout: Timeout      = 4.seconds
}
