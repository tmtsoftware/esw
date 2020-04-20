package esw.sm.core

import akka.Done
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import esw.commons.Timeouts
import esw.commons.utils.location.LocationServiceUtil
import esw.sm.messages.ConfigureResponse._
import esw.sm.messages.SequenceManagerMsg._
import esw.sm.messages.{ConfigureResponse, SequenceManagerMsg}
import esw.sm.utils.SequencerUtil

import scala.async.Async.{async, await}
import scala.concurrent.Future

class SequenceManagerBehavior(
    config: Map[String, ObsModeConfig],
    locationServiceUtil: LocationServiceUtil,
    sequencerUtil: SequencerUtil
)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  implicit val timeout: Timeout = Timeouts.DefaultTimeout

  def behavior(): Behavior[SequenceManagerMsg] = Behaviors.setup(_ => idle())

  def idle(): Behavior[SequenceManagerMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Configure(observingMode, replyTo) => configure(observingMode, ctx.self); configuring(replyTo);
      case GetRunningObsModes(replyTo)       => getRunningObsModes.map(replyTo ! _); Behaviors.same
      case Cleanup(observingMode, replyTo)   => Behaviors.same
      case _                                 => Behaviors.same
    }
  }

  def cleaningUp(replyTo: ActorRef[Done]): Behavior[SequenceManagerMsg] = receive[CleanupCompleted.type] { _ =>
    replyTo ! Done
    idle()
  }

  def cleanup(obsMode: String, self: ActorRef[SequenceManagerMsg]): Unit =
    sequencerUtil
      .stopSequencers(extractSequencers(obsMode), obsMode)
      .onComplete(_ => self ! CleanupCompleted) // ignore clean up failure

  def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] = receive[ConfigurationCompleted] { msg =>
    replyTo ! msg.res
    idle()
  }

  def configure(obsMode: String, self: ActorRef[SequenceManagerMsg]): Unit =
    async {
      val mayBeOcsMaster: Option[HttpLocation] = await(sequencerUtil.resolveMasterSequencerOf(obsMode))
      val response: ConfigureResponse = mayBeOcsMaster match {
        case Some(location) => await(useOcsMaster(location, obsMode))
        // todo : check all needed sequencer are idle. also handle case of partial start up
        case None => await(configureResources(obsMode, configuredObsModes = await(getRunningObsModes)))
      }

      self ! ConfigurationCompleted(response)
    }

  def useOcsMaster(location: HttpLocation, obsMode: String): Future[ConfigureResponse] = async {
    if (await(sequencerUtil.areSequencersIdle(extractSequencers(obsMode), obsMode))) Success(location)
    else ConfigurationFailure(s"Error: ${location.prefix} is already executing another sequence")
  }

  def configureResources(obsMode: String, configuredObsModes: Set[String]): Future[ConfigureResponse] = async {
    val requiredResources: Resources        = extractResources(obsMode)
    val configuredResources: Set[Resources] = configuredObsModes.map(extractResources)
    val areResourcesConflicting             = configuredResources.exists(_.conflictsWith(requiredResources))

    if (areResourcesConflicting) ConflictingResourcesWithRunningObsMode
    else await(sequencerUtil.startSequencers(obsMode, extractSequencers(obsMode)))
  }

  def getRunningObsModes: Future[Set[String]]        = locationServiceUtil.listBy(ESW, Sequencer).map(_.map(getObsMode).toSet)
  def getObsMode(akkaLocation: AkkaLocation): String = akkaLocation.prefix.componentName

  def extractSequencers(obsMode: String): Sequencers = config(obsMode).sequencers
  def extractResources(obsMode: String): Resources   = config(obsMode).resources

  def receive[T <: SequenceManagerMsg](handler: T => Behavior[SequenceManagerMsg]): Behavior[SequenceManagerMsg] =
    Behaviors.receiveMessage {
      case GetRunningObsModes(replyTo) => getRunningObsModes.map(replyTo ! _); Behaviors.same // common msg
      case msg: T                      => handler(msg)
      case _                           => Behaviors.unhandled
    }
}
