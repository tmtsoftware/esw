package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.actor.messages.{CommonMessage, SequenceManagerIdleMsg, SequenceManagerMsg, UnhandleableSequenceManagerMsg}
import esw.sm.api.models.ObsModeStatus.{Configurable, Configured, NonConfigurable}
import esw.sm.api.models.SequenceManagerState.{Idle, Processing}
import esw.sm.api.models.{ProvisionConfig, SequenceManagerState, _}
import esw.sm.api.protocol.ConfigureResponse.{ConfigurationMissing, ConflictingResourcesWithRunningObsMode}
import esw.sm.api.protocol.StartSequencerResponse.AlreadyRunning
import esw.sm.api.protocol.{ResourceStatusResponse, _}
import esw.sm.impl.config.{ObsModeConfig, SequenceManagerConfig}
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.chaining.scalaUtilChainingOps

class SequenceManagerBehavior(
    sequenceManagerConfig: SequenceManagerConfig,
    locationServiceUtil: LocationServiceUtil,
    agentUtil: AgentUtil,
    sequencerUtil: SequencerUtil,
    sequenceComponentUtil: SequenceComponentUtil
)(implicit val actorSystem: ActorSystem[_], implicit val logger: Logger) {

  import SequenceManagerBehavior._
  import actorSystem.executionContext

  def setup: SMBehavior = Behaviors.setup(ctx => idle(ctx.self))

  private def idle(self: SelfRef): SMBehavior =
    receive[SequenceManagerIdleMsg](Idle) {
      case Configure(obsMode, replyTo) => configure(obsMode, self, replyTo)
      case Provision(config, replyTo)  => provision(config, self, replyTo)

      // Shutdown sequencers
      case ShutdownSequencer(subsystem, obsMode, replyTo) =>
        sequencerUtil.shutdownSequencer(subsystem, obsMode).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownSubsystemSequencers(subsystem, replyTo) =>
        sequencerUtil.shutdownSubsystemSequencers(subsystem).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownObsModeSequencers(obsMode, replyTo) =>
        sequencerUtil.shutdownObsModeSequencers(obsMode).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownAllSequencers(replyTo) =>
        sequencerUtil.shutdownAllSequencers().map(self ! ProcessingComplete(_)); processing(self, replyTo)

      case StartSequencer(subsystem, obsMode, replyTo)   => startSequencer(obsMode, subsystem, self, replyTo)
      case RestartSequencer(subsystem, obsMode, replyTo) => restartSequencer(subsystem, obsMode, self, replyTo)

      case ShutdownSequenceComponent(prefix, replyTo) =>
        sequenceComponentUtil.shutdownSequenceComponent(prefix).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownAllSequenceComponents(replyTo) =>
        sequenceComponentUtil.shutdownAllSequenceComponents().map(self ! ProcessingComplete(_)); processing(self, replyTo)
    }

  private def configure(obsMode: ObsMode, self: SelfRef, replyTo: ActorRef[ConfigureResponse]): SMBehavior = {
    // getRunningObsModes finds the currently running observation modes
    val runningObsModesF = getRunningObsModes.flatMapToAdt(
      configuredObsModes => configureResources(obsMode, configuredObsModes),
      error => CommonFailure.LocationServiceError(error.msg)
    )

    runningObsModesF.map(self ! ProcessingComplete(_))
    processing(self, replyTo)
  }

  // start all the required sequencers associated with obs mode,
  // if requested resources does not conflict with existing running observations
  private def configureResources(requestedObsMode: ObsMode, runningObsModes: Set[ObsMode]): Future[ConfigureResponse] =
    async {
      // get obsMode config for requested observation mode from sequence manager config
      sequenceManagerConfig.obsModeConfig(requestedObsMode) match {
        // check for resource conflict between requested obsMode and currently running obsMode
        case Some(ObsModeConfig(resources, _)) if isNonConfigurable(resources, runningObsModes) =>
          ConflictingResourcesWithRunningObsMode(runningObsModes)
        case Some(ObsModeConfig(_, sequencers)) =>
          await(sequencerUtil.startSequencers(requestedObsMode, sequencers))
        case None => ConfigurationMissing(requestedObsMode)
      }
    }

  // ignoring failure of getResources as config should never be absent for running obsModes
  private def isNonConfigurable(requiredResources: Resources, runningObsModes: Set[ObsMode]) =
    requiredResources.conflictsWithAny(runningObsModes.map(getResources))

  private def getResources(obsMode: ObsMode): Resources = sequenceManagerConfig.resources(obsMode).get

  private def startSequencer(
      obsMode: ObsMode,
      subsystem: Subsystem,
      self: SelfRef,
      replyTo: ActorRef[StartSequencerResponse]
  ): SMBehavior = {
    // resolve is not needed here. Find should suffice
    // no concurrent start sequencer or configure is allowed
    locationServiceUtil
      .find(HttpConnection(ComponentId(Prefix(subsystem, obsMode.name), Sequencer)))
      .flatMap {
        case Left(_)         => sequenceComponentUtil.loadScript(subsystem, obsMode)
        case Right(location) => Future.successful(AlreadyRunning(location.connection.componentId))
      }
      .map(self ! ProcessingComplete(_))
    processing(self, replyTo)
  }

  private def restartSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      self: SelfRef,
      replyTo: ActorRef[RestartSequencerResponse]
  ): SMBehavior = {
    val restartResponseF = sequencerUtil.restartSequencer(subsystem, obsMode)
    restartResponseF.map(self ! ProcessingComplete(_))
    processing(self, replyTo)
  }

  private def provision(config: ProvisionConfig, self: SelfRef, replyTo: ActorRef[ProvisionResponse]): SMBehavior = {
    // shutdown all running seq comps and then provision the new once
    sequenceComponentUtil.shutdownAllSequenceComponents().map {
      case ShutdownSequenceComponentResponse.Success          => agentUtil.provision(config).map(self ! ProcessingComplete(_))
      case failure: ShutdownSequenceComponentResponse.Failure => self ! ProcessingComplete(failure)
    }

    processing(self, replyTo)
  }

  // processing some message, waiting for ProcessingComplete message
  // Within this period, reject all the other messages except common messages
  private def processing[T <: SmResponse](self: SelfRef, replyTo: ActorRef[T]): SMBehavior =
    receive[ProcessingComplete[T]](Processing)(msg => replyAndGoToIdle(self, replyTo, msg.res))

  private def replyAndGoToIdle[T](self: SelfRef, replyTo: ActorRef[T], msg: T) = {
    msg match {
      case failure: SmFailure => logger.error(s"Sequence Manager response Error: ${failure.getMessage}")
      case success            => logger.info(s"Sequence Manager response Success: ${success}")
    }
    replyTo ! msg
    idle(self)
  }

  private def receive[T <: SequenceManagerMsg: ClassTag](state: SequenceManagerState)(handler: T => SMBehavior): SMBehavior =
    Behaviors.receiveMessage { msg =>
      logger.debug(s"Sequence Manager in State: $state, received Message: $msg")
      msg match {
        case msg: CommonMessage => handleCommon(msg, state); Behaviors.same
        case msg: T             => handler(msg)
        case msg: UnhandleableSequenceManagerMsg =>
          msg.replyTo ! Unhandled(state.entryName, msg.getClass.getSimpleName)
          Behaviors.same
        case x => throw new MatchError(x)
      }
    }

  private def buildResourceStatusList(runningObsModes: Set[ObsMode]) = {
    val obsModes           = sequenceManagerConfig.obsModes.toSet
    val resourceToObsMode  = obsModes.flatMap(kv => kv._2.resources.resources.map(r => (r, kv._1)))
    val (inUse, available) = resourceToObsMode.partition(t => runningObsModes.contains(t._2))

    val inUseResources = inUse.map {
      case (resource, mode) => ResourceStatusResponse(resource, ResourceStatus.InUse, Some(mode))
    }
    val availableResources = available.filterNot(t => inUse.exists(_._1 == t._1)).map(t => ResourceStatusResponse(t._1))
    inUseResources ++ availableResources
  }

  private def getResourcesStatus(replyTo: ActorRef[ResourcesStatusResponse]): Future[Unit] =
    getRunningObsModes.mapToAdt(
      runningObsModes => {
        val resourceStatus = buildResourceStatusList(runningObsModes)
        logger.info(s"Sequence Manager response Success: $resourceStatus")
        replyTo ! ResourcesStatusResponse.Success(resourceStatus.toList)
      },
      error => {
        logger.error(s"Sequence Manager response Failure: ${error.getMessage}")
        replyTo ! ResourcesStatusResponse.Failed(error.getMessage)
      }
    )

  private def handleCommon(msg: CommonMessage, currentState: SequenceManagerState): Unit =
    msg match {
      case GetSequenceManagerState(replyTo) =>
        currentState.tap(state => logger.info(s"Sequence Manager response Success: Sequence Manager state $state"));
        replyTo ! currentState
      case GetResources(replyTo)       => getResourcesStatus(replyTo)
      case GetObsModesDetails(replyTo) => getObsModesDetails.foreach(replyTo ! _)
    }

  private def getObsModesDetails: Future[ObsModesDetailsResponse] = {
    def getObsModeStatus(obsMode: ObsMode, resources: Resources, configuredObsModes: Set[ObsMode]): ObsModeStatus = {
      if (configuredObsModes.contains(obsMode)) Configured
      else if (isNonConfigurable(resources, configuredObsModes)) NonConfigurable
      else Configurable
    }

    getRunningObsModes.mapToAdt(
      configuredObsModes => {
        val obsModes = sequenceManagerConfig.obsModes.toSet
        val obsModesDetails = obsModes.map {
          case (obsMode, ObsModeConfig(resources, sequencers)) =>
            ObsModeDetails(obsMode, getObsModeStatus(obsMode, resources, configuredObsModes), resources, sequencers)
        }

        val response = ObsModesDetailsResponse.Success(obsModesDetails)
        logger.info(s"Sequence Manager response Success: $response")
        response
      },
      error => {
        logger.error(s"Sequence Manager response Error: ${error.getMessage}")
        CommonFailure.LocationServiceError(error.msg)
      }
    )
  }

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
