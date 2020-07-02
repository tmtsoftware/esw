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
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState._
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.actor.messages.{SequenceManagerIdleMsg, SequenceManagerMsg}
import esw.sm.api.protocol.CommonFailure.ConfigurationMissing
import esw.sm.api.protocol.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.protocol.StartSequencerResponse.{AlreadyRunning, Started}
import esw.sm.api.protocol._
import esw.sm.impl.config.{ObsModeConfig, Resources, SequenceManagerConfig}
import esw.sm.impl.utils.{SequenceComponentUtil, SequencerUtil}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

class SequenceManagerBehavior(
    config: SequenceManagerConfig,
    locationServiceUtil: LocationServiceUtil,
    sequencerUtil: SequencerUtil,
    sequenceComponentUtil: SequenceComponentUtil
)(implicit val actorSystem: ActorSystem[_]) {
  import SequenceManagerBehavior._
  import actorSystem.executionContext

  private val sequencerStartRetries = config.sequencerStartRetries

  def setup: SMBehavior = Behaviors.setup(ctx => idle(ctx.self))

  private def idle(self: SelfRef): SMBehavior =
    receive[SequenceManagerIdleMsg](Idle) {
      case Configure(observingMode, replyTo)                 => configure(observingMode, self, replyTo)
      case Cleanup(observingMode, replyTo)                   => cleanup(observingMode, self, replyTo)
      case StartSequencer(subsystem, observingMode, replyTo) => startSequencer(subsystem, observingMode, replyTo); Behaviors.same
      case ShutdownSequencer(subsystem, observingMode, replyTo) =>
        shutdownSequencer(subsystem, observingMode, replyTo); Behaviors.same
      case ShutdownAllSequencers(replyTo) => shutdownAllSequencers(replyTo); Behaviors.same
      case RestartSequencer(subsystem, observingMode, replyTo) =>
        restartSequencer(subsystem, observingMode, replyTo); Behaviors.same
      case SpawnSequenceComponent(machine, name, replyTo) => spawnSequenceComponent(machine, name, replyTo); Behaviors.same
      case ShutdownSequenceComponent(subsystem, componentName, replyTo) =>
        shutdownSequenceComponent(subsystem, componentName, replyTo); Behaviors.same
    }

  private def configure(obsMode: ObsMode, self: SelfRef, replyTo: ActorRef[ConfigureResponse]): SMBehavior = {
    val runningObsModesF = getRunningObsModes.flatMapToAdt(
      configuredObsModes => configureResources(obsMode, configuredObsModes),
      error => CommonFailure.LocationServiceError(error.msg)
    )

    runningObsModesF.map(self ! ConfigurationResponseInternal(_))
    configuring(self, replyTo)
  }

  // start all the required sequencers associated with obs mode,
  // if requested resources does not conflict with existing running observations
  private def configureResources(requestedObsMode: ObsMode, runningObsModes: Set[ObsMode]): Future[ConfigureResponse] =
    async {
      config.obsModeConfig(requestedObsMode) match {
        case Some(ObsModeConfig(resources, _)) if checkConflicts(resources, runningObsModes) =>
          ConflictingResourcesWithRunningObsMode(runningObsModes)
        case Some(ObsModeConfig(_, sequencers)) =>
          await(sequencerUtil.startSequencers(requestedObsMode, sequencers, sequencerStartRetries))
        case None => ConfigurationMissing(requestedObsMode)
      }
    }

  // ignoring failure of getResources as config should never be absent for running obsModes
  private def checkConflicts(requiredResources: Resources, runningObsModes: Set[ObsMode]) =
    requiredResources.conflictsWithAny(runningObsModes.map(getResources))

  private def getResources(obsMode: ObsMode): Resources = config.resources(obsMode).get

  // Configuration is in progress, waiting for ConfigurationResponseInternal message
  // Within this period, reject all the other messages except common messages
  private def configuring(self: SelfRef, replyTo: ActorRef[ConfigureResponse]): SMBehavior =
    receive[ConfigurationResponseInternal](Configuring)(msg => replyAndGoToIdle(self, replyTo, msg.res))

  private def cleanup(obsMode: ObsMode, self: SelfRef, replyTo: ActorRef[CleanupResponse]): SMBehavior = {
    val cleanupResponseF =
      config
        .sequencers(obsMode)
        .map(sequencerUtil.shutdownSequencers(_, obsMode))
        .getOrElse(Future.successful(ConfigurationMissing(obsMode)))

    cleanupResponseF.map(self ! CleanupResponseInternal(_))
    cleaningUp(self, replyTo)
  }

  // Clean up is in progress, waiting for CleanupResponseInternal message
  // Within this period, reject all the other messages except common messages
  private def cleaningUp(self: SelfRef, replyTo: ActorRef[CleanupResponse]): SMBehavior =
    receive[CleanupResponseInternal](CleaningUp)(msg => replyAndGoToIdle(self, replyTo, msg.res))

  private def startSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      replyTo: ActorRef[StartSequencerResponse]
  ): Future[Unit] = {
    // resolve is not needed here. Find should suffice
    // no concurrent start sequencer or configure is allowed
    locationServiceUtil
      .find(HttpConnection(ComponentId(Prefix(subsystem, obsMode.name), Sequencer)))
      .flatMap {
        case Left(_)         => startSequencer(subsystem, obsMode)
        case Right(location) => Future.successful(AlreadyRunning(location.connection.componentId))
      }
      .map(replyTo ! _)
  }

  private def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    sequencerUtil
      .startSequencer(subsystem, obsMode, sequencerStartRetries)
      .mapToAdt(akkaLocation => Started(akkaLocation.connection.componentId), identity)

  private def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      replyTo: ActorRef[ShutdownSequencerResponse]
  ): Future[Unit] = {
    val shutdownResponseF = sequencerUtil.shutdownSequencer(subsystem, obsMode).mapToAdt(identity, identity)
    shutdownResponseF.map(replyTo ! _)
  }

  private def restartSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      replyTo: ActorRef[RestartSequencerResponse]
  ): Future[Unit] = {
    val restartResponseF = sequencerUtil.restartSequencer(subsystem, obsMode)
    restartResponseF.map(replyTo ! _)
  }

  private def shutdownAllSequencers(replyTo: ActorRef[ShutdownAllSequencersResponse]): Future[Unit] =
    sequencerUtil.shutdownAllSequencers().map(replyTo ! _)

  private def spawnSequenceComponent(
      machine: ComponentId,
      name: String,
      replyTo: ActorRef[SpawnSequenceComponentResponse]
  ): Future[Unit] = {
    sequenceComponentUtil.spawnSequenceComponent(machine, name).map(replyTo ! _)
  }

  private def shutdownSequenceComponent(
      subsystem: Subsystem,
      componentName: String,
      replyTo: ActorRef[ShutdownSequenceComponentResponse]
  ): Future[Unit] = {
    sequenceComponentUtil.shutdown(Prefix(subsystem, componentName)).map(replyTo ! _)
  }

  private def replyAndGoToIdle[T](self: SelfRef, replyTo: ActorRef[T], msg: T) = {
    replyTo ! msg
    idle(self)
  }

  private def receive[T <: SequenceManagerMsg: ClassTag](state: SequenceManagerState)(handler: T => SMBehavior): SMBehavior =
    Behaviors.receiveMessage {
      case msg: CommonMessage => handleCommon(msg, state); Behaviors.same
      case msg: T             => handler(msg)
      case _                  => Behaviors.unhandled
    }

  private def handleCommon(msg: CommonMessage, currentState: SequenceManagerState): Unit =
    msg match {
      case GetRunningObsModes(replyTo)      => runningObsModesResponse.foreach(replyTo ! _)
      case GetSequenceManagerState(replyTo) => replyTo ! currentState
    }

  private def runningObsModesResponse =
    getRunningObsModes.mapToAdt(
      obsModes => GetRunningObsModesResponse.Success(obsModes),
      error => GetRunningObsModesResponse.Failed(error.msg)
    )

  // get the component name of all the top level sequencers i.e. ESW sequencers
  private def getRunningObsModes: Future[Either[RegistrationListingFailed, Set[ObsMode]]] =
    locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer).mapRight(_.map(getObsMode).toSet)

  // componentName = obsMode, as per convention, sequencer uses obs mode to form component name
  private def getObsMode(akkaLocation: AkkaLocation): ObsMode = ObsMode(akkaLocation.prefix.componentName)
}

object SequenceManagerBehavior {
  type SMBehavior = Behavior[SequenceManagerMsg]
  type SelfRef    = ActorRef[SequenceManagerMsg]
}
