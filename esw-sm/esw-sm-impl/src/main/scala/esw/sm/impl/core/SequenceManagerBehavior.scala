package esw.sm.impl.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId}
import csw.logging.api.scaladsl.Logger
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.Prefix
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.models.ObsMode
import esw.sm.api.actor.messages.SequenceManagerMsg.*
import esw.sm.api.actor.messages.{CommonMessage, SequenceManagerIdleMsg, SequenceManagerMsg, UnhandleableSequenceManagerMsg}
import esw.sm.api.models.*
import esw.sm.api.models.ObsModeStatus.{Configurable, Configured, NonConfigurable}
import esw.sm.api.models.SequenceManagerState.{Idle, Processing}
import esw.sm.api.protocol.*
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.{ConfigurationMissing, ConflictingResourcesWithRunningObsMode}
import esw.sm.api.protocol.StartSequencerResponse.AlreadyRunning
import esw.sm.impl.config.{ObsModeConfig, SequenceManagerConfig}
import esw.sm.impl.utils.Types.{AgentLocation, SequencerPrefix}
import esw.sm.impl.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.reflect.ClassTag
import scala.util.chaining.scalaUtilChainingOps
import scala.util.control.NonFatal

/*
 * This a behavior class for the sequence manager actor. This behavior class is base on State machine means behavior depends on the state of sequence manager.
 *
 * sequenceManagerConfig - sequence manager config for the sequence manager
 * locationServiceUtil - an instance of [[esw.commons.utils.location.LocationServiceUtil]]
 * agentUtil - an instance of [[esw.commons.utils.location.LocationServiceUtil]]
 * sequencerUtil - an instance of [[esw.sm.impl.utils.SequencerUtil]]
 * sequenceComponentUtil - an instance of [[esw.sm.impl.utils.SequenceComponentUtil]]
 * actorSystem - an Akka ActorSystem
 * logger - a logger for Logging
 */
class SequenceManagerBehavior(
    sequenceManagerConfig: SequenceManagerConfig,
    locationServiceUtil: LocationServiceUtil,
    agentUtil: AgentUtil,
    sequencerUtil: SequencerUtil,
    sequenceComponentUtil: SequenceComponentUtil
)(implicit val actorSystem: ActorSystem[_], implicit val logger: Logger) {

  import SequenceManagerBehavior.*
  import actorSystem.executionContext

  def setup: SMBehavior = Behaviors.setup(ctx => idle(ctx.self))

  /*
   * Returns the behavior of sequence manager in Idle state
   * In this state all the [[SequenceManagerIdleMsg]] as well as common messages are accepted.
   *
   * self -> actor ref of sequence manager itself
   */
  private def idle(self: SelfRef) =
    receive[SequenceManagerIdleMsg](Idle) {
      case Configure(obsMode, replyTo) => configure(obsMode, self, replyTo)
      case Provision(config, replyTo)  => provision(config, self, replyTo)

      // Shutdown sequencers
      case ShutdownSequencer(prefix, replyTo) =>
        sequencerUtil
          .shutdownSequencer(prefix)
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownSequencer](self)
        processing(self, replyTo)

      case ShutdownSubsystemSequencers(subsystem, replyTo) =>
        sequencerUtil
          .shutdownSubsystemSequencers(subsystem)
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownSubsystemSequencers](self)
        processing(self, replyTo)

      case ShutdownObsModeSequencers(obsMode, replyTo) =>
        sequencerUtil
          .shutdownObsModeSequencers(obsMode)
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownObsModeSequencers](self)
        processing(self, replyTo)

      case ShutdownAllSequencers(replyTo) =>
        sequencerUtil
          .shutdownAllSequencers()
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownAllSequencers](self)
        processing(self, replyTo)

      case StartSequencer(prefix, replyTo)   => startSequencer(prefix, self, replyTo)
      case RestartSequencer(prefix, replyTo) => restartSequencer(prefix, self, replyTo)

      case ShutdownSequenceComponent(prefix, replyTo) =>
        sequenceComponentUtil
          .shutdownSequenceComponent(prefix)
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownSequenceComponent](self)

        processing(self, replyTo)

      case ShutdownAllSequenceComponents(replyTo) =>
        sequenceComponentUtil
          .shutdownAllSequenceComponents()
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownAllSequenceComponents](self)
        processing(self, replyTo)
    }

  private def configure(obsMode: ObsMode, self: SelfRef, replyTo: ActorRef[ConfigureResponse]): SMBehavior = {
    // getRunningObsModes finds the currently running observation modes
    val runningObsModesF = getRunningObsModes.flatMapToAdt(
      configuredObsModes => configureResources(obsMode, configuredObsModes),
      error => LocationServiceError(error.msg)
    )
    runningObsModesF
      .map(self ! ProcessingComplete(_))
      .recoverWithProcessingError[Configure](self)
    processing(self, replyTo)

  }

  // start all the required sequencers associated with obs mode,
  // if requested resources does not conflict with existing running observations
  private def configureResources(requestedObsMode: ObsMode, runningObsModes: Set[ObsMode]): Future[ConfigureResponse] =
    async {
      // get obsMode config for requested observation mode from sequence manager config
      sequenceManagerConfig.obsModeConfig(requestedObsMode) match {
        // check for resource conflict between requested obsMode and currently running obsMode
        case Some(ObsModeConfig(resources, _)) if isConflicting(resources, runningObsModes) =>
          ConflictingResourcesWithRunningObsMode(runningObsModes)
        case Some(ObsModeConfig(_, sequencers)) =>
          await(sequencerUtil.startSequencers(requestedObsMode, sequencers))
        case None => ConfigurationMissing(requestedObsMode)
      }
    }

  // ignoring failure of getResources as config should never be absent for running obsModes
  private def isConflicting(requiredResources: Resources, runningObsModes: Set[ObsMode]) =
    requiredResources.conflictsWithAny(runningObsModes.map(getResources))

  //return Resources of a particular obsMode from the SequenceManagerConfig
  private def getResources(obsMode: ObsMode): Resources = sequenceManagerConfig.resources(obsMode).get

  private def startSequencer(prefix: Prefix, self: SelfRef, replyTo: ActorRef[StartSequencerResponse]) = {

    // resolve is not needed here. Find should suffice
    // no concurrent start sequencer or configure is allowed
    locationServiceUtil
      .find(HttpConnection(ComponentId(prefix, Sequencer)))
      .flatMap {
        case Left(_)         => sequenceComponentUtil.loadScript(prefix)
        case Right(location) => Future.successful(AlreadyRunning(location.connection.componentId))
      }
      .map(self ! ProcessingComplete(_))
      .recoverWithProcessingError[StartSequencer](self)
    processing(self, replyTo)
  }

  private def restartSequencer(prefix: SequencerPrefix, self: SelfRef, replyTo: ActorRef[RestartSequencerResponse]) = {
    val restartResponseF = sequencerUtil.restartSequencer(prefix)
    restartResponseF
      .map(self ! ProcessingComplete(_))
      .recoverWithProcessingError[RestartSequencer](self)
    processing(self, replyTo)
  }

  private def provision(config: ProvisionConfig, self: SelfRef, replyTo: ActorRef[ProvisionResponse]): SMBehavior = {

    // shutdown all running seq comps and then provision the new once
    sequenceComponentUtil
      .shutdownAllSequenceComponents()
      .map {
        case ShutdownSequenceComponentResponse.Success =>
          agentUtil
            .provision(config)
            .map(self ! ProcessingComplete(_))
            .recoverWithProcessingError[Provision](self)
        case failure: ShutdownSequenceComponentResponse.Failure => self ! ProcessingComplete(failure)
      }
      .recoverWithProcessingError[Provision](self)

    processing(self, replyTo)
  }

  /*
   * Returns the behavior of sequence manager in Processing state state.
   * In this state, actor is processing some message, waiting for ProcessingComplete message
   * Within this period, it rejects all the other messages except common messages
   *
   * self -> actor ref of sequence manager itself
   * replyTo -> actor ref of the actor whom to send the ProcessingComplete received in this state
   */
  private def processing[T <: SmResponse](
      self: SelfRef,
      replyTo: ActorRef[T]
  ): SMBehavior =
    receive[ProcessingComplete[T]](Processing)(msg => replyAndGoToIdle(self, replyTo, msg.res))

  private def replyAndGoToIdle[T](self: SelfRef, replyTo: ActorRef[T], msg: T) = {
    msg match {
      case failure: SmFailure => logger.error(s"Sequence Manager response Error: ${failure.getMessage} ******* $msg")
      case success            => logger.info(s"Sequence Manager response Success: $success")
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

  /*
   * Return the set of all the resources' status(In Use and Available)
   *
   * If a resources is required for running obsMode then it is marked as InUse resource
   * otherwise it is an available resource
   */
  private def buildResourceStatusList(runningObsModes: Set[ObsMode]) = {
    val obsModes           = sequenceManagerConfig.obsModes.toSet
    val resourceToObsMode  = obsModes.flatMap(kv => kv._2.resources.resources.map(r => (r, kv._1)))
    val (inUse, available) = resourceToObsMode.partition(t => runningObsModes.contains(t._2))

    val inUseResources = inUse.map { case (resource, mode) =>
      ResourceStatusResponse(resource, ResourceStatus.InUse, Some(mode))
    }
    val availableResources = available.filterNot(t => inUse.exists(_._1 == t._1)).map(t => ResourceStatusResponse(t._1))
    inUseResources ++ availableResources
  }

  /*
   * gets all the running obsMode and uses it to determine the status of resources by using buildResourceStatusList method
   */
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

  /*
   * Handler for the messages common to each state of the sequence manager
   */
  private def handleCommon(msg: CommonMessage, currentState: SequenceManagerState): Unit =
    msg match {
      case GetSequenceManagerState(replyTo) =>
        currentState.tap(state => logger.info(s"Sequence Manager response Success: Sequence Manager state $state"))
        replyTo ! currentState
      case GetResources(replyTo)       => getResourcesStatus(replyTo)
      case GetObsModesDetails(replyTo) => getObsModesDetails.foreach(replyTo ! _)

    }

  /*
   * Gets status of the given obsMode
   */
  def getObsModeStatus(
      obsMode: ObsMode,
      obsModeConfig: ObsModeConfig,
      configuredObsModes: Set[ObsMode],
      allIdleSequenceComps: List[AgentLocation]
  ): ObsModeStatus = {
    def allocateSequenceComponents(sequencerPrefixes: List[SequencerPrefix]) = {
      val allocator = sequenceComponentUtil.sequenceComponentAllocator
      val subsystemSpecificIdleSeqComps =
        allIdleSequenceComps.filter(location => sequencerPrefixes.map(_.subsystem).contains(location.prefix.subsystem))
      allocator.allocate(subsystemSpecificIdleSeqComps, sequencerPrefixes)
    }

    if (configuredObsModes.contains(obsMode)) Configured
    else {
      val conflicting          = isConflicting(obsModeConfig.resources, configuredObsModes)
      val prefixes             = obsModeConfig.sequencers.sequencerIds.map(_.prefix(obsMode))
      val missingSequenceComps = allocateSequenceComponents(prefixes)
      missingSequenceComps.fold(
        e => NonConfigurable(e.sequencerPrefixes),
        _ => if (conflicting) NonConfigurable(Nil) else Configurable
      )
    }
  }

  private def getObsModesDetails: Future[ObsModesDetailsResponse] = {
    val runningObsModes        = getRunningObsModes.mapLeft(e => LocationServiceError(e.msg))
    val idleSequenceComponents = sequenceComponentUtil.getAllIdleSequenceComponents

    val response =
      runningObsModes
        .flatMapE(obsModes => idleSequenceComponents.mapRight(locs => (obsModes, locs)))
        .mapRight { case (configuredObsModes, locs) =>
          val obsModes = sequenceManagerConfig.obsModes.toSet
          val obsModesStatus =
            obsModes.map { case (obsMode, cfg @ ObsModeConfig(resources, sequencers)) =>
              val obsMOdeStatus = getObsModeStatus(obsMode, cfg, configuredObsModes, locs)
              ObsModeDetails(obsMode, obsMOdeStatus, resources, sequencers)
            }

          val response = ObsModesDetailsResponse.Success(obsModesStatus)
          logger.info(s"Sequence Manager response Success: $response")
          response
        }

    response.mapToAdt(
      identity,
      e => e.tap(_ => logger.error(s"Sequence Manager response Error: ${e.getMessage}"))
    )
  }

  // get the component name of all the top level sequencers i.e. ESW sequencers
  private def getRunningObsModes: Future[Either[RegistrationListingFailed, Set[ObsMode]]] =
    locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer).mapRight(_.map(getObsMode).toSet)

  // componentName = obsMode, as per convention, sequencer uses obs mode to form component name
  private def getObsMode(akkaLocation: AkkaLocation): ObsMode = ObsMode(akkaLocation.prefix.componentName)

  private implicit class FutureOps[T](private val future: Future[T]) {

    /*
     * This method is created for sending ProcessingComplete message with the failure to sequence manager actor whenever the future fails
     */
    def recoverWithProcessingError[Msg <: SequenceManagerMsg: ClassTag](selfRef: SelfRef): Future[Any] = {
      val msg = scala.reflect.classTag[Msg].runtimeClass.getSimpleName
      future.recover { case NonFatal(ex) =>
        val reason = s"Sequence Manager Operation($msg) failed due to: ${ex.getMessage}"
        selfRef ! ProcessingComplete(FailedResponse(reason))
      }
    }
  }
}

object SequenceManagerBehavior {
  type SMBehavior = Behavior[SequenceManagerMsg]
  type SelfRef    = ActorRef[SequenceManagerMsg]
}
