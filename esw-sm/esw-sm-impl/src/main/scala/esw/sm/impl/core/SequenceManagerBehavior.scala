package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.ComponentType.{Machine, Sequencer}
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
import esw.sm.api.actor.messages.{CommonMessage, SequenceManagerIdleMsg, SequenceManagerMsg}
import esw.sm.api.protocol.AgentStatusResponses.AgentStatus
import esw.sm.api.protocol.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.protocol.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.protocol.StartSequencerResponse.AlreadyRunning
import esw.sm.api.protocol._
import esw.sm.impl.config.{ObsModeConfig, ProvisionConfig, Resources, SequenceManagerConfig}
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.{Failure, Success}

class SequenceManagerBehavior(
    sequenceManagerConfig: SequenceManagerConfig,
    provisionConfigProvider: () => Future[ProvisionConfig],
    locationServiceUtil: LocationServiceUtil,
    agentUtil: AgentUtil,
    sequencerUtil: SequencerUtil,
    sequenceComponentUtil: SequenceComponentUtil
)(implicit val actorSystem: ActorSystem[_]) {

  import SequenceManagerBehavior._
  import actorSystem.executionContext

  def setup: SMBehavior = Behaviors.setup(ctx => idle(ctx.self))

  private def idle(self: SelfRef): SMBehavior =
    receive[SequenceManagerIdleMsg](Idle) {
      case Configure(obsMode, replyTo) => configure(obsMode, self, replyTo)

      // Shutdown sequencers
      case ShutdownSequencer(subsystem, obsMode, replyTo) =>
        sequencerUtil.shutdownSequencer(subsystem, obsMode).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownSubsystemSequencers(subsystem, replyTo) =>
        sequencerUtil.shutdownSubsystemSequencers(subsystem).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownObsModeSequencers(obsMode, replyTo) =>
        sequencerUtil.shutdownObsModeSequencers(obsMode).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownAllSequencers(replyTo) =>
        sequencerUtil.shutdownAllSequencers().map(self ! ProcessingComplete(_)); processing(self, replyTo)

      case StartSequencer(subsystem, obsMode, replyTo)    => startSequencer(obsMode, subsystem, self, replyTo)
      case RestartSequencer(subsystem, obsMode, replyTo)  => restartSequencer(subsystem, obsMode, self, replyTo)
      case SpawnSequenceComponent(machine, name, replyTo) => spawnSequenceComponent(machine, name, self, replyTo)

      case ShutdownSequenceComponent(prefix, replyTo) =>
        sequenceComponentUtil.shutdownSequenceComponent(prefix).map(self ! ProcessingComplete(_)); processing(self, replyTo)
      case ShutdownAllSequenceComponents(replyTo) =>
        sequenceComponentUtil.shutdownAllSequenceComponents().map(self ! ProcessingComplete(_)); processing(self, replyTo)

      case Provision(replyTo) => provision(self, replyTo)
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
        case Some(ObsModeConfig(resources, _)) if checkConflicts(resources, runningObsModes) =>
          ConflictingResourcesWithRunningObsMode(runningObsModes)
        case Some(ObsModeConfig(_, sequencers)) =>
          await(sequencerUtil.startSequencers(requestedObsMode, sequencers))
        case None => ConfigurationMissing(requestedObsMode)
      }
    }

  // ignoring failure of getResources as config should never be absent for running obsModes
  private def checkConflicts(requiredResources: Resources, runningObsModes: Set[ObsMode]) =
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

  private def spawnSequenceComponent(
      machine: Prefix,
      name: String,
      self: SelfRef,
      replyTo: ActorRef[SpawnSequenceComponentResponse]
  ): SMBehavior = {
    agentUtil.spawnSequenceComponent(machine, name).map(self ! ProcessingComplete(_))
    processing(self, replyTo)
  }

  private def provision(self: SelfRef, replyTo: ActorRef[ProvisionResponse]): SMBehavior = {
    provisionConfigProvider()
      .onComplete {
        case Failure(err)   => self ! ProcessingComplete(ProvisionResponse.ConfigurationFailure(err.getMessage))
        case Success(value) => agentUtil.provision(value).map(self ! ProcessingComplete(_))
      }

    processing(self, replyTo)
  }

  // processing some message, waiting for ProcessingComplete message
  // Within this period, reject all the other messages except common messages
  private def processing[T <: SmResponse](self: SelfRef, replyTo: ActorRef[T]): SMBehavior =
    receive[ProcessingComplete[T]](Processing)(msg => replyAndGoToIdle(self, replyTo, msg.res))

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
      case GetAgentStatus(replyTo)          => getAgentStatus(replyTo)
    }

  // todo: Extract in class
  private def getAgentStatus(
      replyTo: ActorRef[AgentStatusResponse]
  ): Future[Unit] = {
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .flatMapRight { agentLocations =>
        agentUtil.getSequenceComponentsRunningOn(agentLocations).flatMap { agentToSeqCompsList =>
          Future.traverse(agentToSeqCompsList) { agentToSeqComp =>
            sequenceComponentUtil
              .getSequenceComponentStatus(agentToSeqComp.seqComps)
              .map(seqCompStatus => AgentStatus(agentToSeqComp.agentId, seqCompStatus))
          }
        }
      }
      .mapToAdt(agentStatusList => AgentStatusResponse.Success(agentStatusList), e => LocationServiceError(e.msg))
      .map(replyTo ! _)
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
