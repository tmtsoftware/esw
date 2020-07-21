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
import esw.sm.api.protocol.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.protocol.ConfigureResponse.ConflictingResourcesWithRunningObsMode
import esw.sm.api.protocol.StartSequencerResponse.AlreadyRunning
import esw.sm.api.protocol._
import esw.sm.impl.config.{ObsModeConfig, ProvisionConfig, Resources, SequenceManagerConfig}
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag

class SequenceManagerBehavior(
    obsModeConfig: SequenceManagerConfig,
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
      case Configure(obsMode, replyTo)                    => configure(obsMode, self, replyTo)
      case ShutdownSequencers(policy, replyTo)            => shutdownSequencers(policy, self, replyTo)
      case StartSequencer(subsystem, obsMode, replyTo)    => startSequencer(obsMode, subsystem, self, replyTo)
      case RestartSequencer(subsystem, obsMode, replyTo)  => restartSequencer(subsystem, obsMode, self, replyTo)
      case SpawnSequenceComponent(machine, name, replyTo) => spawnSequenceComponent(machine, name, self, replyTo)
      case ShutdownSequenceComponents(policy, replyTo)    => shutdownSequenceComponents(policy, self, replyTo)
      case Provision(replyTo)                             => provision(self, replyTo)
    }

  private def configure(obsMode: ObsMode, self: SelfRef, replyTo: ActorRef[ConfigureResponse]): SMBehavior = {
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
      obsModeConfig.obsModeConfig(requestedObsMode) match {
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

  private def getResources(obsMode: ObsMode): Resources = obsModeConfig.resources(obsMode).get

  private def shutdownSequencers(
      policy: ShutdownSequencersPolicy,
      self: SelfRef,
      replyTo: ActorRef[ShutdownSequencersResponse]
  ): SMBehavior = {
    sequencerUtil.shutdownSequencers(policy).map(self ! ProcessingComplete(_))
    processing(self, replyTo)
  }

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
        case Left(_)         => sequenceComponentUtil.loadScript(subsystem, obsMode).mapToAdt(identity, identity)
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

  private def shutdownSequenceComponents(
      policy: ShutdownSequenceComponentsPolicy,
      self: SelfRef,
      replyTo: ActorRef[ShutdownSequenceComponentResponse]
  ): SMBehavior = {
    sequenceComponentUtil.shutdown(policy).map(self ! ProcessingComplete(_))
    processing(self, replyTo)
  }

  private def provision(self: SelfRef, replyTo: ActorRef[ProvisionResponse]): SMBehavior = {
    provisionConfigProvider()
      .flatMap(agentUtil.provision)
      .recover(err => ProvisionResponse.ConfigurationFailure(err.getMessage))
      .map(self ! ProcessingComplete(_))

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
      replyTo: ActorRef[GetAgentStatusResponse]
  ): Future[Unit] = {
    locationServiceUtil
      .listAkkaLocationsBy(Machine)
      .flatMapRight { agentLocations =>
        agentUtil.getSequenceComponentsRunningOn(agentLocations).flatMap { agentToSeqCompMap =>
          Future.traverse(agentToSeqCompMap.toList) {
            case (agent, seqComps) =>
              sequenceComponentUtil.getSequenceComponentStatus(seqComps).map(seqCompStatus => agent -> seqCompStatus)
          }
        }
      }
      .mapToAdt(mapping => GetAgentStatusResponse.Success(mapping.toMap), e => LocationServiceError(e.msg))
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
