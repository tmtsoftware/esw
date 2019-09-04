package esw.ocs.admin.impl

import akka.actor.typed.ActorSystem
import akka.util.Timeout
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import esw.ocs.admin.api.SequencerAdminApi
import esw.ocs.api.models.StepList
import esw.ocs.client.SequencerAdminClient
import esw.ocs.client.messages.SequencerMessages.EswSequencerMessage

import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SequencerAdminImpl(locationService: LocationService)(implicit actorSystem: ActorSystem[_], timeout: Timeout)
    extends SequencerAdminApi {

  import actorSystem.executionContext

  private def resolve(sequencerName: String) =
    locationService
      .resolve(AkkaConnection(ComponentId(sequencerName, ComponentType.Sequencer)), 5.seconds)
      .map(_.getOrElse(throw new RuntimeException("wew")))
      .map(loc => new SequencerAdminClient(loc.uri.toActorRef.unsafeUpcast[EswSequencerMessage]))

  override def getSequence(sequencerName: String): Future[Option[StepList]] =
    resolve(sequencerName).flatMap(client => client.getSequence)

//  def isAvailable: Future[Boolean] = ???
//
//  def isOnline: Future[Boolean] = ???
//
//  def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] = ???
//
//  def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] = ???
//
//  def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = ???
//
//  def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = ???
//
//  def delete(id: Id): Future[GenericResponse] = ???
//
//  def pause: Future[PauseResponse] = ???
//
//  def resume: Future[OkOrUnhandledResponse] = ???
//
//  def addBreakpoint(id: Id): Future[GenericResponse] = ???
//
//  def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] = ???
//
//  def reset(): Future[OkOrUnhandledResponse] = ???
//
//  def abortSequence(): Future[OkOrUnhandledResponse] = ???
//
//  def goOnline(): Future[GoOnlineResponse] = ???
//
//  def goOffline(): Future[OkOrUnhandledResponse] = ???
}
