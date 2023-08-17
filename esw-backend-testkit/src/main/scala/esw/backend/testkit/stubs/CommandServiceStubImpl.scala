package esw.backend.testkit.stubs

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.Timeout
import csw.command.api.StateMatcher
import csw.command.api.scaladsl.CommandService
import csw.location.api.scaladsl.LocationService
import csw.params.commands.CommandIssue.IdNotAvailableIssue
import csw.params.commands.CommandResponse.*
import csw.params.commands.{CommandResponse, ControlCommand, Result}
import csw.params.core.generics.KeyType.IntKey
import csw.params.core.models.Id
import csw.params.core.states.{CurrentState, StateName}
import csw.prefix.models.Prefix
import esw.ocs.testkit.utils.LocationUtils
import msocket.api.Subscription

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.{DurationLong, FiniteDuration}

class CommandServiceStubImpl(val locationService: LocationService, _actorSystem: ActorSystem[SpawnProtocol.Command])
    extends CommandService
    with LocationUtils {

  private val delayDuration: FiniteDuration        = 2.seconds
  private val crm: mutable.Map[Id, SubmitResponse] = mutable.Map()

  override implicit def actorSystem: ActorSystem[SpawnProtocol.Command] = _actorSystem

  override def validate(controlCommand: ControlCommand): Future[CommandResponse.ValidateResponse] =
    Future.successful(Accepted(Id()))

  override def submit(controlCommand: ControlCommand): Future[CommandResponse.SubmitResponse] = {
    val runId        = Id()
    val numbersParam = IntKey.make("numbers").set(1, 2, 3)
    future(delayDuration, Completed(runId, Result.emptyResult.add(numbersParam))).map(res => crm.put(runId, res))
    val res = Started(runId)
    crm.put(runId, res)
    Future.successful(res)
  }

  override def oneway(controlCommand: ControlCommand): Future[CommandResponse.OnewayResponse] = Future.successful(Accepted(Id()))

  override def query(commandRunId: Id): Future[CommandResponse.SubmitResponse] =
    Future.successful(crm.getOrElse(commandRunId, Invalid(commandRunId, IdNotAvailableIssue(""))))

  override def queryFinal(commandRunId: Id)(implicit timeout: Timeout): Future[CommandResponse.SubmitResponse] =
    future(delayDuration, ()).flatMap(_ => query(commandRunId))

  override def subscribeCurrentState(names: Set[StateName]): Source[CurrentState, Subscription] = {
    val currentState1 = CurrentState(Prefix("esw.a.b"), StateName("stateName1"))
    val currentState2 = CurrentState(Prefix("esw.a.b"), StateName("stateName2"))
    val futureStream  = future(2.seconds, Source(List(currentState1, currentState2).filter(c => names.contains(c.stateName))))

    Source.futureSource(futureStream).mapMaterializedValue(_ => () => ())
  }

  // Unimplemented as they are not supported over http
  override def subscribeCurrentState(callback: CurrentState => Unit): Subscription = ???

  override def subscribeCurrentState(names: Set[StateName], callback: CurrentState => Unit): Subscription = ???

  override def onewayAndMatch(
      controlCommand: ControlCommand,
      stateMatcher: StateMatcher
  ): Future[CommandResponse.MatchingResponse] = ???

  // Following methods are unimplemented as these will not have any corresponding msg at Gateway side
  // They are implemented using client side composition
  override def submitAllAndWait(submitCommands: List[ControlCommand])(implicit
      timeout: Timeout
  ): Future[List[CommandResponse.SubmitResponse]] = ???

  override def submitAndWait(
      controlCommand: ControlCommand
  )(implicit timeout: Timeout): Future[CommandResponse.SubmitResponse] = ???

}
