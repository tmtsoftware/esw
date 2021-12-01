package esw.ocs.api.actor.client

import akka.actor.typed.scaladsl.AskPattern.*
import akka.actor.typed.{ActorRef, ActorSystem}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.models.AkkaLocation
import csw.prefix.models.Subsystem
import esw.constants.SequenceComponentTimeouts
import esw.ocs.api.SequenceComponentApi
import esw.ocs.api.actor.messages.SequenceComponentMsg
import esw.ocs.api.actor.messages.SequenceComponentMsg.{GetStatus, LoadScript, RestartScript, Shutdown, UnloadScript}
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.api.protocol.SequenceComponentResponse.{GetStatusResponse, Ok, ScriptResponseOrUnhandled}

import scala.concurrent.Future

/**
 * Actor client for the sequence component. This client's apis sends message to sequence component's actor
 * and returned the response provided by it
 * This client takes actor ref of the sequence component as a constructor argument
 *
 * @param sequenceComponentLocation - location of the sequence component
 * @param actorSystem - an Akka ActorSystem
 */
class SequenceComponentImpl(sequenceComponentLocation: AkkaLocation)(implicit
    actorSystem: ActorSystem[_]
) extends SequenceComponentApi {

  private val sequenceComponentRef = sequenceComponentLocation.uri.toActorRef.unsafeUpcast[SequenceComponentMsg]

  override def loadScript(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[ScriptResponseOrUnhandled] =
    (sequenceComponentRef ? { x: ActorRef[ScriptResponseOrUnhandled] => LoadScript(subsystem, obsMode, variation, x) })(
      SequenceComponentTimeouts.LoadScript,
      actorSystem.scheduler
    )

  override def restartScript(): Future[ScriptResponseOrUnhandled] =
    (sequenceComponentRef ? RestartScript)(SequenceComponentTimeouts.RestartScript, actorSystem.scheduler)

  override def status: Future[GetStatusResponse] =
    (sequenceComponentRef ? GetStatus)(SequenceComponentTimeouts.Status, actorSystem.scheduler)

  override def unloadScript(): Future[Ok.type] =
    (sequenceComponentRef ? UnloadScript)(SequenceComponentTimeouts.UnloadScript, actorSystem.scheduler)

  override def shutdown(): Future[Ok.type] =
    (sequenceComponentRef ? Shutdown)(SequenceComponentTimeouts.Shutdown, actorSystem.scheduler)
}
