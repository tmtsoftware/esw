package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Subsystem.ESW
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningInProcess, ConfigurationInProcess, Idle}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.models.ConfigureResponse.{ConfigurationFailure, ConflictingResourcesWithRunningObsMode}
import esw.sm.api.models._
import esw.sm.impl.utils.SequencerUtil

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

class SequenceManagerBehavior(
    config: Map[String, ObsModeConfig],
    locationServiceUtil: LocationServiceUtil,
    sequencerUtil: SequencerUtil
)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  implicit val timeout: Timeout = Timeouts.DefaultTimeout

  //todo: try to use common receive method
  def idle(): Behavior[SequenceManagerMsg] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Configure(observingMode, replyTo)     => configure(observingMode, ctx.self); configuring(replyTo);
        case GetRunningObsModes(replyTo)           => replyRunningObsMode(replyTo); Behaviors.same
        case Cleanup(observingMode, replyTo)       => cleanup(observingMode, ctx.self); cleaningUp(replyTo);
        case GetSequenceManagerState(replyTo)      => replyTo ! Idle; Behaviors.same
        case _: CleanupDone | _: ConfigurationDone => Behaviors.unhandled
      }
    }

  private def cleanup(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      val stopResponseF = sequencerUtil
        .stopSequencers(extractSequencers(obsMode), obsMode)
        .mapToAdt(_ => CleanupResponse.Success, error => CleanupResponse.Failed(error.msg))

      self ! CleanupDone(await(stopResponseF))
    }

  private def cleaningUp(replyTo: ActorRef[CleanupResponse]): Behavior[SequenceManagerMsg] =
    receive[CleanupDone](CleaningInProcess) { msg =>
      replyTo ! msg.res
      idle()
    }

  private def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] =
    receive[ConfigurationDone](ConfigurationInProcess) { msg =>
      replyTo ! msg.res
      idle()
    }

  private def configure(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      val runningObsModesF = getRunningObsModes.flatMapToAdt(
        configuredObsModes => configureResources(obsMode, configuredObsModes),
        error => ConfigurationFailure(error.msg)
      )

      self ! ConfigurationDone(await(runningObsModesF))
    }

  private def configureResources(obsMode: String, configuredObsModes: Set[String]): Future[ConfigureResponse] =
    async {
      val requiredResources: Resources        = extractResources(obsMode)
      val configuredResources: Set[Resources] = configuredObsModes.map(extractResources)
      val areResourcesConflicting             = configuredResources.exists(_.conflictsWith(requiredResources))

      if (areResourcesConflicting) ConflictingResourcesWithRunningObsMode
      else await(sequencerUtil.startSequencers(obsMode, extractSequencers(obsMode)))
    }

  private def getRunningObsModes: Future[Either[RegistrationListingFailed, Set[String]]] =
    locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer).mapRight(_.map(getObsMode).toSet)

  private def getObsMode(akkaLocation: AkkaLocation): String = akkaLocation.prefix.componentName

  private def extractSequencers(obsMode: String): Sequencers = config(obsMode).sequencers
  private def extractResources(obsMode: String): Resources   = config(obsMode).resources

  private def replyRunningObsMode(replyTo: ActorRef[GetRunningObsModesResponse]) =
    getRunningObsModes.map {
      case Left(value)  => replyTo ! GetRunningObsModesResponse.Failed(value.msg)
      case Right(value) => replyTo ! GetRunningObsModesResponse.Success(value)
    }

  private def receive[T <: SequenceManagerMsg: ClassTag](
      state: SequenceManagerState
  )(handler: T => Behavior[SequenceManagerMsg]): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage {
      case GetRunningObsModes(replyTo)      => replyRunningObsMode(replyTo); Behaviors.same // common msg
      case GetSequenceManagerState(replyTo) => replyTo ! state; Behaviors.same
      case msg: T                           => handler(msg)
      case _                                => Behaviors.unhandled
    }
}
