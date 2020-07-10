package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.sm.api.SequenceManagerState
import esw.sm.api.SequenceManagerState._
import esw.sm.api.actor.messages.SequenceManagerMsg._
import esw.sm.api.actor.messages.{SequenceManagerIdleMsg, SequenceManagerMsg}
import esw.sm.api.protocol.CommonFailure.{ConfigurationMissing, LocationServiceError}
import esw.sm.api.protocol.ConfigureResponse.{ConflictingResourcesWithRunningObsMode, SequenceComponentsNotAvailable}
import esw.sm.api.protocol.StartSequencerResponse.{AlreadyRunning, SequenceComponentNotAvailable}
import esw.sm.api.protocol._
import esw.sm.impl.config.{ObsModeConfig, Resources, SequenceManagerConfig, Sequencers}
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

  def setup: SMBehavior = Behaviors.setup(ctx => idle(ctx.self))

  private def idle(self: SelfRef): SMBehavior =
    receive[SequenceManagerIdleMsg](Idle) {
      case Configure(obsMode, replyTo)                    => configure(obsMode, self, replyTo)
      case ShutdownSequencers(policy, replyTo)            => shutdownSequencers(policy, self, replyTo)
      case StartSequencer(subsystem, obsMode, replyTo)    => startSequencer(obsMode, subsystem, self, replyTo)
      case RestartSequencer(subsystem, obsMode, replyTo)  => restartSequencer(subsystem, obsMode, self, replyTo)
      case SpawnSequenceComponent(machine, name, replyTo) => spawnSequenceComponent(machine, name, self, replyTo)
      case ShutdownSequenceComponents(policy, replyTo)    => shutdownSequenceComponents(policy, self, replyTo)
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
      config.obsModeConfig(requestedObsMode) match {
        case Some(ObsModeConfig(resources, _)) if checkConflicts(resources, runningObsModes) =>
          ConflictingResourcesWithRunningObsMode(runningObsModes)
        case Some(ObsModeConfig(_, sequencers)) =>
          await(sequencerUtil.startSequencers(requestedObsMode, sequencers))
        case None => ConfigurationMissing(requestedObsMode)
      }
    }

  def startSequencers(
      obsMode: ObsMode,
      sequencers: Sequencers
  ): Future[Either[ConfigureResponse.Failure, List[Either[StartSequencerResponse.Failure, StartSequencerResponse.Started]]]] = {
    mapSequencersToSequenceComponents(sequencers).flatMapRight(mappings => {
      val responsesF = Future.traverse(mappings) { mapping =>
        val (subsystem, seqCompLocation) = mapping
        sequenceComponentUtil.loadScript(subsystem, obsMode, seqCompLocation)
      }
      responsesF.map(_.sequence)
    })
  }

  def mapSequencersToSequenceComponents(
      sequencers: Sequencers
  ): Future[Either[ConfigureResponse.Failure, List[(Subsystem, AkkaLocation)]]] = {
    val responseF = sequenceComponentUtil
      .idleSequenceComponentsFor(sequencers.subsystems)
      .mapRight(locations => {
        var startWithLocations = locations
        for {
          subsystem <- sequencers.subsystems
          seqComp   <- findMatchingSeqComp(subsystem, startWithLocations)
        } yield {
          startWithLocations = startWithLocations.filterNot(_.equals(seqComp))
          (subsystem -> seqComp)
        }
      })

    responseF.map {
      case Left(error)     => Left(LocationServiceError(error.msg))
      case Right(mappings) => Right(mappings)
    }
  }

  def mapSequencersToSequenceComponents1(
      sequencers: Sequencers
  ): Future[Either[ConfigureResponse.Failure, List[(Subsystem, AkkaLocation)]]] =
    async {
      val response = await(sequenceComponentUtil.idleSequenceComponentsFor(sequencers.subsystems))
      response match {
        case Left(error) => Left(LocationServiceError(error.msg))
        case Right(locations) =>
          sequencers.subsystems.map(s => {
            findMatchingSeqComp(s, locations)
          })
      }
      ???
    }

  def dd(subsystems: List[Subsystem], seqCompLocations: List[AkkaLocation]) = {
    subsystems match {
      case ::(subsystem, remainingSubsystems) =>
        findMatchingSeqComp(subsystem, seqCompLocations) match {
          case Some(location) => (subsystem, location) :: dd(remainingSubsystems, seqCompLocations.filterNot(_.equals(location)))
          case None           => Left()
        }
      case Nil =>
    }
  }

  def findMatchingSeqComp(subsystem: Subsystem, seqCompLocations: List[AkkaLocation]): Option[AkkaLocation] =
    seqCompLocations.find(_.prefix.subsystem == subsystem).orElse(seqCompLocations.find(_.prefix.subsystem == ESW))

  // ignoring failure of getResources as config should never be absent for running obsModes
  private def checkConflicts(requiredResources: Resources, runningObsModes: Set[ObsMode]) =
    requiredResources.conflictsWithAny(runningObsModes.map(getResources))

  private def getResources(obsMode: ObsMode): Resources = config.resources(obsMode).get

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
    sequenceComponentUtil.spawnSequenceComponent(machine, name).map(self ! ProcessingComplete(_))
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
