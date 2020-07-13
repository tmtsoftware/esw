package esw.sm.impl.utils

import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, ComponentId, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.extensions.ListEitherExt.ListEitherOps
import esw.commons.utils.FutureUtils
import esw.commons.utils.location.{EswLocationError, LocationServiceUtil}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{SequencerLocation, Unhandled}
import esw.sm.api.protocol.CommonFailure.LocationServiceError
import esw.sm.api.protocol.ConfigureResponse.FailedToStartSequencers
import esw.sm.api.protocol.ShutdownSequencersPolicy.{AllSequencers, ObsModeSequencers, SingleSequencer, SubsystemSequencers}
import esw.sm.api.protocol.StartSequencerResponse.{LoadScriptError, SequenceComponentNotAvailable}
import esw.sm.api.protocol._
import esw.sm.impl.config.Sequencers

import scala.async.Async.{async, await}
import scala.concurrent.{ExecutionContext, Future}

class SequencerUtil(locationServiceUtil: LocationServiceUtil, sequenceComponentUtil: SequenceComponentUtil)(implicit
    actorSystem: ActorSystem[_]
) {
  implicit private val ec: ExecutionContext = actorSystem.executionContext

  def createMappingAndStartSequencers(obsMode: ObsMode, sequencers: Sequencers): Future[ConfigureResponse] =
    async {
      val response = await(sequenceComponentUtil.idleSequenceComponentsFor(sequencers.subsystems))
      response match {
        case Left(error) => LocationServiceError(error.msg)
        case Right(locations) =>
          await(mapSequencersToSequenceComponents(sequencers.subsystems, locations) match {
            case Left(error)     => Future.successful(error)
            case Right(mappings) => startSequencers(obsMode, mappings)
          })
      }
    }

  def restartSequencer(subSystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    locationServiceUtil
      .findSequencer(subSystem, obsMode)
      .flatMapToAdt(restartSequencer, e => LocationServiceError(e.msg))

  def shutdownSequencers(policy: ShutdownSequencersPolicy): Future[ShutdownSequencersResponse] =
    policy match {
      case SingleSequencer(subsystem, obsMode) => shutdownSequencersAndHandleErrors(getSequencer(subsystem, obsMode))
      case SubsystemSequencers(subsystem)      => shutdownSequencersAndHandleErrors(getSubsystemSequencers(subsystem))
      case ObsModeSequencers(obsMode)          => shutdownSequencersAndHandleErrors(getObsModeSequencers(obsMode))
      case AllSequencers                       => shutdownSequencersAndHandleErrors(getAllSequencers)
    }

  private def startSequencers(obsMode: ObsMode, mappings: List[(Subsystem, AkkaLocation)]): Future[ConfigureResponse] = {
    val responsesF = Future.traverse(mappings) { mapping =>
      val (subsystem, seqCompLocation) = mapping
      sequenceComponentUtil.loadScript(subsystem, obsMode, seqCompLocation)
    }
    responsesF
      .map(_.sequence)
      .mapToAdt(
        _ => {
          val masterSequencerId = ComponentId(Prefix(ESW, obsMode.name), Sequencer)
          ConfigureResponse.Success(masterSequencerId)
        },
        errors => FailedToStartSequencers(errors.map(_.msg).toSet)
      )
  }

  private def mapSequencersToSequenceComponents(
      subsystems: List[Subsystem],
      seqCompLocations: List[AkkaLocation]
  ): Either[SequenceComponentNotAvailable, List[(Subsystem, AkkaLocation)]] = {
    subsystems match {
      case Nil => Right(List.empty)
      case ::(subsystem, remainingSubsystems) =>
        findMatchingSeqComp(subsystem, seqCompLocations) match {
          case None => Left(SequenceComponentNotAvailable("adequate amount of sequence components not available"))
          case Some(location) =>
            mapSequencersToSequenceComponents(remainingSubsystems, seqCompLocations.filterNot(_.equals(location))).map(list =>
              list :+ (subsystem, location)
            )
        }
    }
  }

  private def findMatchingSeqComp(subsystem: Subsystem, seqCompLocations: List[AkkaLocation]): Option[AkkaLocation] =
    seqCompLocations.find(_.prefix.subsystem == subsystem).orElse(seqCompLocations.find(_.prefix.subsystem == ESW))

  private def restartSequencer(akkaLocation: AkkaLocation): Future[RestartSequencerResponse] =
    createSequencerClient(akkaLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.restartScript(_).map {
        case SequencerLocation(location) => RestartSequencerResponse.Success(location.connection.componentId)
        case error: ScriptError          => LoadScriptError(error.msg)
        case Unhandled(_, _, msg)        => LoadScriptError(msg) // restart is unhandled in idle or shutting down state
      })

  private def getSequencer(subsystem: Subsystem, obsMode: ObsMode) =
    locationServiceUtil.findSequencer(subsystem, obsMode).mapRight(List(_))
  private def getSubsystemSequencers(subsystem: Subsystem) = locationServiceUtil.listAkkaLocationsBy(subsystem, Sequencer)
  private def getObsModeSequencers(obsMode: ObsMode)       = locationServiceUtil.listAkkaLocationsBy(obsMode.name, Sequencer)
  private def getAllSequencers                             = locationServiceUtil.listAkkaLocationsBy(Sequencer)

  private def shutdownSequencersAndHandleErrors(sequencers: Future[Either[EswLocationError, List[AkkaLocation]]]) =
    sequencers.flatMapRight(unloadScripts).mapToAdt(identity, locationErrorToShutdownSequencersResponse)

  private def locationErrorToShutdownSequencersResponse(err: EswLocationError) =
    err match {
      case _: EswLocationError.LocationNotFound => ShutdownSequencersResponse.Success
      case e: EswLocationError                  => LocationServiceError(e.msg)
    }

  // get sequence component from Sequencer and unload sequencer script
  private def unloadScript(sequencerLocation: AkkaLocation) =
    createSequencerClient(sequencerLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.unloadScript)
      .map(_ => ShutdownSequencersResponse.Success)

  private def unloadScripts(sequencerLocations: List[AkkaLocation]): Future[ShutdownSequencersResponse.Success.type] =
    Future.traverse(sequencerLocations)(unloadScript).map(_ => ShutdownSequencersResponse.Success)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)

  private def sequential[T, L, R](i: List[T])(f: T => Future[Either[L, R]]) = FutureUtils.sequential(i)(f).map(_.sequence)
}
