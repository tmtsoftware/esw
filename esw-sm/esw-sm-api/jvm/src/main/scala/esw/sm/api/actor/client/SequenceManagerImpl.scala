package esw.sm.api.actor.client

import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem}
import akka.util.Timeout
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg.{Cleanup, Configure, GetRunningObsModes}
import esw.sm.api.models.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse}

import scala.concurrent.Future

//fixme: replace smRef with akkaLocation once location service registration story is played
class SequenceManagerImpl(smRef: ActorRef[SequenceManagerMsg])(implicit
    actorSystem: ActorSystem[_],
    timeout: Timeout
) extends SequenceManagerApi {

  override def configure(observingMode: String): Future[ConfigureResponse] = smRef ? (Configure(observingMode, _))

  override def cleanup(observingMode: String): Future[CleanupResponse] = smRef ? (Cleanup(observingMode, _))

  override def getRunningObsModes: Future[GetRunningObsModesResponse] = smRef ? GetRunningObsModes
}
