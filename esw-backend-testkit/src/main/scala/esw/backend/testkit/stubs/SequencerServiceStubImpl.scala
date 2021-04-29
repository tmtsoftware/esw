package esw.backend.testkit.stubs

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.scaladsl.LocationService
import csw.params.commands.CommandResponse.{Started, SubmitResponse}
import csw.params.commands.{CommandName, Sequence, SequenceCommand, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol._
import esw.ocs.testkit.utils.LocationUtils
import msocket.api.Subscription
import msocket.jvm.SourceExtension.WithSubscription

import scala.concurrent.Future

class SequencerServiceStubImpl(val locationService: LocationService, _actorSystem: ActorSystem[SpawnProtocol.Command])
    extends SequencerApi
    with LocationUtils {

  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem

  private val runId: Id      = Id("123")
  private val stepList       = StepList(Sequence(Setup(Prefix("CSW.IRIS"), CommandName("command-1"), None)))
  private val sequencerState = ExternalSequencerState.Running

  override def loadSequence(sequence: Sequence): Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def startSequence(): Future[SubmitResponse] = Future.successful(Started(runId))

  override def getSequence: Future[Option[StepList]] = Future.successful(Some(stepList))

  override def add(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def prepend(commands: List[SequenceCommand]): Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def replace(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = Future.successful(Ok)

  override def insertAfter(id: Id, commands: List[SequenceCommand]): Future[GenericResponse] = Future.successful(Ok)

  override def delete(id: Id): Future[GenericResponse] = Future.successful(Ok)

  override def addBreakpoint(id: Id): Future[GenericResponse] = Future.successful(Ok)

  override def removeBreakpoint(id: Id): Future[RemoveBreakpointResponse] = Future.successful(Ok)

  override def reset(): Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def pause: Future[PauseResponse] = Future.successful(Ok)

  override def resume: Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def getSequenceComponent: Future[AkkaLocation] = ???

  override def isAvailable: Future[Boolean] = Future.successful(true)

  override def isOnline: Future[Boolean] = Future.successful(true)

  override def goOnline(): Future[GoOnlineResponse] = Future.successful(Ok)

  override def goOffline(): Future[GoOfflineResponse] = Future.successful(Ok)

  override def abortSequence(): Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def stop(): Future[OkOrUnhandledResponse] = Future.successful(Ok)

  override def diagnosticMode(startTime: UTCTime, hint: String): Future[DiagnosticModeResponse] = Future.successful(Ok)

  override def operationsMode(): Future[OperationsModeResponse] = Future.successful(Ok)

  override def submit(sequence: Sequence): Future[SubmitResponse] = Future.successful(Started(runId))

  override def submitAndWait(sequence: Sequence)(implicit timeout: Timeout): Future[SubmitResponse] =
    Future.successful(Started(runId))

  override def query(runId: Id): Future[SubmitResponse] = Future.successful(Started(runId))

  override def queryFinal(runId: Id)(implicit timeout: Timeout): Future[SubmitResponse] = Future.successful(Started(runId))

  override def getSequencerState: Future[ExternalSequencerState] = Future.successful(sequencerState)

  override def subscribeSequencerState(): Source[SequencerStateResponse, Subscription] =
    Source(List(SequencerStateResponse(stepList, sequencerState))).withSubscription()

}
