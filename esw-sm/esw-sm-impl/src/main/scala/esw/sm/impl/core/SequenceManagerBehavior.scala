package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.AkkaLocation
import csw.location.api.models.ComponentType.Sequencer
import csw.prefix.models.Subsystem.ESW
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningInProcess, ConfigurationInProcess, Idle}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.models.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.models.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.models._
import esw.sm.impl.utils.SequencerUtil

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

class SequenceManagerBehavior(
    config: SequenceManagerConfig,
    locationServiceUtil: LocationServiceUtil,
    sequencerUtil: SequencerUtil
)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext

  def idle(): Behavior[SequenceManagerMsg] =
    Behaviors.receive { (ctx, msg) =>
      msg match {
        case Configure(observingMode, replyTo)             => configure(observingMode, ctx.self); configuring(replyTo);
        case Cleanup(observingMode, replyTo)               => cleanup(observingMode, ctx.self); cleaningUp(replyTo);
        case msg: CommonMessage                            => handleCommon(msg, Idle); Behaviors.same
        case _: CleanupInternal | _: ConfigurationInternal => Behaviors.unhandled
      }
    }

  // Clean up is in progress, waiting for CleanupDone message
  // Within this period, reject all the other messages except common messages
  private def cleaningUp(replyTo: ActorRef[CleanupResponse]): Behavior[SequenceManagerMsg] =
    receive[CleanupInternal](CleaningInProcess) { msg =>
      replyTo ! msg.res
      idle()
    }

  // Configuration is in progress, waiting for ConfigurationDone message
  // Within this period, reject all the other messages except common messages
  private def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] =
    receive[ConfigurationInternal](ConfigurationInProcess) { msg =>
      replyTo ! msg.res
      idle()
    }

  private def handleCommon(msg: CommonMessage, currentState: SequenceManagerState): Unit =
    msg match {
      case GetRunningObsModes(replyTo)      => runningObsModesResponse.foreach(replyTo ! _)
      case GetSequenceManagerState(replyTo) => replyTo ! currentState
    }

  private def configure(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      val runningObsModesF = getRunningObsModes.flatMapToAdt(
        configuredObsModes => configureResources(obsMode, configuredObsModes),
        error => LocationServiceError(error.msg)
      )

      self ! ConfigurationInternal(await(runningObsModesF))
    }

  private def cleanup(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      config.sequencers(obsMode) match {
        case Left(_) =>
          self ! CleanupInternal(ConfigurationMissing(obsMode))
        case Right(sequencers) =>
          val stopResponseF = sequencerUtil
            .stopSequencers(sequencers, obsMode)
            .mapToAdt(_ => CleanupResponse.Success, error => LocationServiceError(error.msg))
          self ! CleanupInternal(await(stopResponseF))
      }
    }

  // start all the required sequencers associated with obs mode,
  // if requested resources does not conflict with existing running observations
  private def configureResources(requestedObsMode: String, runningObsModes: Set[String]): Future[ConfigureResponse] =
    async {
      config.resources(requestedObsMode) match {
        case Left(_) => ConfigurationMissing(requestedObsMode)
        case Right(requiredResources) =>
          val configuredResources: Set[Resources] = runningObsModes.map(om => {
            config.resources(om) match {
              // ignoring failure of getResources as config should never be absent for running obsModes
              case Right(resources) => resources
            }
          })
          if (configuredResources.exists(_.conflictsWith(requiredResources)))
            ConflictingResourcesWithRunningObsMode(runningObsModes)
          else {
            config.sequencers(requestedObsMode) match {
              case Left(_)      => ConfigurationMissing(requestedObsMode)
              case Right(value) => await(sequencerUtil.startSequencers(requestedObsMode, value))
            }
          }
      }
    }

  // get the component name of all the top level sequencers i.e. ESW sequencers
  private def getRunningObsModes: Future[Either[RegistrationListingFailed, Set[String]]] =
    locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer).mapRight(_.map(getObsMode).toSet)

  // componentName = obsMode, as per convention, sequencer uses obs mode to form component name
  private def getObsMode(akkaLocation: AkkaLocation): String = akkaLocation.prefix.componentName

  private def runningObsModesResponse =
    getRunningObsModes.mapToAdt(
      obsModes => GetRunningObsModesResponse.Success(obsModes),
      error => GetRunningObsModesResponse.Failed(error.msg)
    )

  private def receive[T <: SequenceManagerMsg: ClassTag](
      state: SequenceManagerState
  )(handler: T => Behavior[SequenceManagerMsg]): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage {
      case msg: CommonMessage => handleCommon(msg, state); Behaviors.same
      case msg: T             => handler(msg)
      case _                  => Behaviors.unhandled
    }
}
