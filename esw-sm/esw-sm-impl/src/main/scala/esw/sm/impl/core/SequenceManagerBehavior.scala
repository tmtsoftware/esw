package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState.{CleaningUp, Configuring, Idle, StartingSequencer}
import esw.sm.api.actor.messages.SequenceManagerMsg
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.models.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.models.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.models.StartSequencerResponse.{AlreadyRunning, Started}
import esw.sm.api.models._
import esw.sm.impl.config.{ObsModeConfig, Resources, SequenceManagerConfig, Sequencers}
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
        case Configure(observingMode, replyTo) => configure(observingMode, ctx.self); configuring(replyTo)
        case Cleanup(observingMode, replyTo)   => cleanup(observingMode, ctx.self); cleaningUp(replyTo)
        case StartSequencer(subsystem, observingMode, replyTo) =>
          startSequencer(subsystem, observingMode, ctx.self); startingSequencer(replyTo)
        case msg: CommonMessage => handleCommon(msg, Idle); Behaviors.same
        case _: CleanupResponseInternal | _: ConfigurationResponseInternal | _: StartSequencerResponseInternal =>
          Behaviors.unhandled
      }
    }

  // Clean up is in progress, waiting for CleanupResponseInternal message
  // Within this period, reject all the other messages except common messages
  private def cleaningUp(replyTo: ActorRef[CleanupResponse]): Behavior[SequenceManagerMsg] =
    receive[CleanupResponseInternal](CleaningUp) { msg =>
      replyTo ! msg.res
      idle()
    }

  // Configuration is in progress, waiting for ConfigurationResponseInternal message
  // Within this period, reject all the other messages except common messages
  private def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] =
    receive[ConfigurationResponseInternal](Configuring) { msg =>
      replyTo ! msg.res
      idle()
    }

  // Starting sequencer is in progress, waiting for StartSequencerResponseInternal message
  // Within this period, reject all the other messages except common messages
  private def startingSequencer(replyTo: ActorRef[StartSequencerResponse]): Behavior[SequenceManagerMsg] =
    receive[StartSequencerResponseInternal](StartingSequencer) { msg =>
      replyTo ! msg.res
      idle()
    }

  private def startSequencer(
      subsystem: Subsystem,
      obsMode: String,
      self: ActorRef[SequenceManagerMsg]
  ): Future[Unit] =
    locationServiceUtil
      .resolve(HttpConnection(ComponentId(Prefix(subsystem, obsMode), Sequencer)))
      .flatMap {
        case Left(_)         => startSequencerAndResolve(subsystem, obsMode)
        case Right(location) => Future.successful(AlreadyRunning(location))
      }
      .map(r => self ! StartSequencerResponseInternal(r))

  private def startSequencerAndResolve(subsystem: Subsystem, obsMode: String): Future[StartSequencerResponse] = {
    sequencerUtil
      .startSequencer(subsystem, obsMode, 3)
      .flatMapToAdt(
        _ =>
          locationServiceUtil
            .resolve(HttpConnection(ComponentId(Prefix(subsystem, obsMode), Sequencer)))
            .map {
              case Left(error) => LocationServiceError(error.msg)
              case Right(loc)  => Started(loc)
            },
        error => error
      )
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
        error => CommonFailure.LocationServiceError(error.msg)
      )

      self ! ConfigurationResponseInternal(await(runningObsModesF))
    }

  private def cleanup(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      val cleanupResponse =
        config
          .sequencers(obsMode)
          .map(stopSequencers(_, obsMode))
          .getOrElse(Future.successful(ConfigurationMissing(obsMode)))

      self ! CleanupResponseInternal(await(cleanupResponse))
    }

  // start all the required sequencers associated with obs mode,
  // if requested resources does not conflict with existing running observations
  private def configureResources(requestedObsMode: String, runningObsModes: Set[String]): Future[ConfigureResponse] =
    async {
      config.obsModeConfig(requestedObsMode) match {
        case Some(ObsModeConfig(resources, _)) if checkConflicts(resources, runningObsModes) =>
          ConflictingResourcesWithRunningObsMode(runningObsModes)
        case Some(ObsModeConfig(_, sequencers)) => await(sequencerUtil.startSequencers(requestedObsMode, sequencers))
        case None                               => ConfigurationMissing(requestedObsMode)
      }
    }

  private def checkConflicts(requiredResources: Resources, runningObsModes: Set[String]) = {
    // ignoring failure of getResources as config should never be absent for running obsModes
    val configuredResources = runningObsModes.map(config.resources(_).get)
    configuredResources.exists(_.conflictsWith(requiredResources))
  }

  private def stopSequencers(sequencers: Sequencers, obsMode: String) =
    sequencerUtil
      .stopSequencers(sequencers, obsMode)
      .mapToAdt(_ => CleanupResponse.Success, error => CommonFailure.LocationServiceError(error.msg))

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
