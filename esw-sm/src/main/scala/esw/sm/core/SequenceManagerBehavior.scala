package esw.sm.core

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import esw.commons.Timeouts
import esw.commons.extensions.FutureEitherExt.FutureEitherOps
import esw.commons.utils.location.EswLocationError.RegistrationListingFailed
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.messages.ConfigureResponse._
import esw.sm.messages.SequenceManagerMsg._
import esw.sm.messages.{CleanupResponse, ConfigureResponse, GetRunningObsModesResponse, SequenceManagerMsg}
import esw.sm.utils.{SequencerError, SequencerUtil}

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

  def behavior(): Behavior[SequenceManagerMsg] =
    Behaviors.setup(_ => idle() // initial behavior
    )

  //todo: try to use common receive method
  def idle(): Behavior[SequenceManagerMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Configure(observingMode, replyTo) => configure(observingMode, ctx.self); configuring(replyTo);
      case GetRunningObsModes(replyTo) =>
        getRunningObsModes.map {
          case Left(value)  => replyTo ! GetRunningObsModesResponse.Failed(value.msg)
          case Right(value) => replyTo ! GetRunningObsModesResponse.Success(value)
        }; Behaviors.same
      case Cleanup(observingMode, replyTo) => cleanup(observingMode, replyTo);
      case _                               => Behaviors.unhandled
    }
  }

  def cleanup(obsMode: String, replyTo: ActorRef[CleanupResponse]): Behavior[SequenceManagerMsg] =
    receive[CleanupCompleted.type] { _ =>
      sequencerUtil
        .stopSequencers(extractSequencers(obsMode), obsMode)
        .map {
          case Left(error) => replyTo ! CleanupResponse.Failed(error.msg)
          case Right(_)    => replyTo ! CleanupResponse.Success
        }
      idle()
    }

  def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] =
    receive[ConfigurationResponseInternal] { msg =>
      replyTo ! msg.res
      idle()
    }

  def configure(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      val mayBeOcsMaster: Option[HttpLocation] = await(sequencerUtil.resolveMasterSequencerOf(obsMode))

      val response: ConfigureResponse = mayBeOcsMaster match {
        case Some(location) => await(useOcsMaster(location, obsMode))
        // todo : check all needed sequencer are idle. also handle case of partial start up
        case None =>
          await(getRunningObsModes.flatMap {
            case Left(error)               => Future.successful(ConfigurationFailure(error.msg))
            case Right(configuredObsModes) => configureResources(obsMode, configuredObsModes)
          })
      }

      self ! ConfigurationResponseInternal(response)
    }

  def useOcsMaster(location: HttpLocation, obsMode: String): Future[ConfigureResponse] = async {
    val sequencerIdleResponse: Either[SequencerError, Done.type] =
      await(sequencerUtil.checkForSequencersAvailability(extractSequencers(obsMode), obsMode))
    sequencerIdleResponse match {
      case Left(error) => ConfigurationFailure(error.msg)
      case Right(_)    => ConfigureResponse.Success(location)
    }
  }

  def configureResources(obsMode: String, configuredObsModes: Set[String]): Future[ConfigureResponse] = async {
    val requiredResources: Resources        = extractResources(obsMode)
    val configuredResources: Set[Resources] = configuredObsModes.map(extractResources)
    val areResourcesConflicting             = configuredResources.exists(_.conflictsWith(requiredResources))

    if (areResourcesConflicting) ConflictingResourcesWithRunningObsMode
    else await(sequencerUtil.startSequencers(obsMode, extractSequencers(obsMode)))
  }

  def getRunningObsModes: Future[Either[RegistrationListingFailed, Set[String]]] =
    locationServiceUtil.listAkkaLocationsBy(ESW, Sequencer).mapRight(_.map(getObsMode).toSet)

  def getObsMode(akkaLocation: AkkaLocation): String = akkaLocation.prefix.componentName

  def extractSequencers(obsMode: String): Sequencers = config(obsMode).sequencers
  def extractResources(obsMode: String): Resources   = config(obsMode).resources

  def receive[T <: SequenceManagerMsg: ClassTag](handler: T => Behavior[SequenceManagerMsg]): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage {
      case GetRunningObsModes(replyTo) =>
        getRunningObsModes.map {
          case Left(value)  => replyTo ! GetRunningObsModesResponse.Failed(value.msg)
          case Right(value) => replyTo ! GetRunningObsModesResponse.Success(value)
        }; Behaviors.same // common msg
      case msg: T => handler(msg)
      case _      => Behaviors.unhandled
    }
}
