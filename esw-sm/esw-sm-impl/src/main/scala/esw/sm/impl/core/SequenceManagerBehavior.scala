package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningInProcess, ConfigurationInProcess, Idle}
import esw.sm.api.actor.messages.ConfigureResponse.{ConfigurationFailure, ConflictingResourcesWithRunningObsMode}
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.actor.messages.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse, SequenceManagerMsg}
import esw.sm.api.models.{ObsModeConfig, Resources, Sequencers}
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

  def init(): Behavior[SequenceManagerMsg] = idle()

  //todo: try to use common receive method
  private def idle(): Behavior[SequenceManagerMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Configure(observingMode, replyTo) => configure(observingMode, ctx.self); configuring(replyTo);
      case GetRunningObsModes(replyTo)       => replyRunningObsMode(replyTo); Behaviors.same
      case Cleanup(observingMode, replyTo)   => cleanup(observingMode, ctx.self); cleaningUp(replyTo);
      case GetSequenceManagerState(replyTo)  => replyTo ! Idle; Behaviors.same
      case _                                 => Behaviors.unhandled
    }
  }

  private def cleanup(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] = async {
    val response = await(
      sequencerUtil
        .stopSequencers(extractSequencers(obsMode), obsMode)
        .map {
          case Left(error) => CleanupResponse.Failed(error.msg)
          case Right(_)    => CleanupResponse.Success
        }
    )

    self ! CleanupDone(response)
  }

  private def cleaningUp(replyTo: ActorRef[CleanupResponse]): Behavior[SequenceManagerMsg] =
    receive[CleanupDone]({ msg =>
      replyTo ! msg.res
      idle()
    }, CleaningInProcess)

  private def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] =
    receive[ConfigurationDone]({ msg =>
      replyTo ! msg.res
      idle()
    }, ConfigurationInProcess)

  private def configure(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      // check if master sequencer is already up
      val mayBeOcsMaster: Either[EswLocationError, HttpLocation] = await(sequencerUtil.resolveMasterSequencerOf(obsMode))

      val response: ConfigureResponse = mayBeOcsMaster match {
        case Right(location) =>
          // check if all sequencers are idle for obsMode
          await(useOcsMaster(location, obsMode))
        // todo: handle case of partial start up

        // configure resources
        case Left(_) =>
          await(getRunningObsModes.flatMap {
            case Left(error)               => Future.successful(ConfigurationFailure(error.msg)) // can't check conflict --> error
            case Right(configuredObsModes) => configureResources(obsMode, configuredObsModes)
          })
      }

      self ! ConfigurationDone(response)
    }

  private def useOcsMaster(location: HttpLocation, obsMode: String): Future[ConfigureResponse] =
    sequencerUtil
      .checkForSequencersAvailability(extractSequencers(obsMode), obsMode)
      .mapToAdt(_ => ConfigureResponse.Success(location), e => ConfigurationFailure(e.msg))

  private def configureResources(obsMode: String, configuredObsModes: Set[String]): Future[ConfigureResponse] = async {
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
      handler: T => Behavior[SequenceManagerMsg],
      state: SequenceManagerState
  ): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage {
      case GetRunningObsModes(replyTo)      => replyRunningObsMode(replyTo); Behaviors.same // common msg
      case GetSequenceManagerState(replyTo) => replyTo ! state; Behaviors.same
      case msg: T                           => handler(msg)
      case _                                => Behaviors.unhandled
    }
}
