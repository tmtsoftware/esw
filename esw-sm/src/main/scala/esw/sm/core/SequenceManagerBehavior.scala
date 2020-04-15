package esw.sm.core

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.util.Timeout
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.{AkkaLocation, HttpLocation}
import csw.prefix.models.Subsystem.ESW
import esw.commons.utils.location.LocationServiceUtil
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.sm.messages.ConfigureResponse._
import esw.sm.messages.SequenceManagerMsg._
import esw.sm.messages.{ConfigureResponse, SequenceManagerMsg}
import esw.sm.utils.{AgentUtil, SequenceComponentUtil, SequencerUtil}

import scala.async.Async.{async, await}
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble

class SequenceManagerBehavior(locationService: LocationServiceUtil)(implicit val actorSystem: ActorSystem[_]) {
  import actorSystem.executionContext
  implicit val timeout: Timeout = 10.seconds

  private val sequenceComponentUtil = new SequenceComponentUtil(locationService, new AgentUtil(locationService))
  private val sequencerUtil         = new SequencerUtil(locationService, sequenceComponentUtil)

  def beh(): Behavior[SequenceManagerMsg] = Behaviors.setup { ctx =>
    idle() // initial behavior
  }

  def idle(): Behavior[SequenceManagerMsg] = Behaviors.receive { (ctx, msg) =>
    msg match {
      case Configure(observingMode, replyTo) => configure(observingMode, ctx.self); configuring(replyTo);
      case GetRunningObsModes(replyTo)       => getRunningObsModes.map(replyTo ! _); Behaviors.same
      case Cleanup(observingMode, replyTo)   => Behaviors.same
      case _                                 => Behaviors.same
    }
  }

  def configuring(replyTo: ActorRef[ConfigureResponse]): Behavior[SequenceManagerMsg] = Behaviors.receiveMessage {
    case ConfigurationCompleted(res) => replyTo ! res; idle()
    case GetRunningObsModes(replyTo) => getRunningObsModes.map(replyTo ! _); Behaviors.same
    case _                           => Behaviors.unhandled
  }

  def configure(obsMode: String, self: ActorRef[SequenceManagerMsg]): Future[Unit] =
    async {
      val configuredObsModes                   = await(getRunningObsModes) // filter master Seqs from all OCS Seqs
      val mayBeOcsMaster: Option[HttpLocation] = await(sequencerUtil.resolveMasterSequencerOf(obsMode))

      val response: ConfigureResponse = mayBeOcsMaster match {
        case Some(location) => await(useOcsMaster(location))
        case None           => await(configureResources(obsMode, configuredObsModes))
      }

      self ! ConfigurationCompleted(response)
    }

  def useOcsMaster(location: HttpLocation): Future[ConfigureResponse] = async {
    if (await(isOcsAvailable(location))) Success(location)
    else ConfigurationFailure(s"Error: ${location.prefix} is already executing another sequence")
  }

  def configureResources(obsMode: String, configuredObsModes: Set[String]): Future[ConfigureResponse] = async {
    val requiredResources: Resources        = extractResources(obsMode)
    val configuredResources: Set[Resources] = configuredObsModes.map(extractResources)
    val areResourcesConflicting             = configuredResources.exists(_.conflictsWith(requiredResources))

    if (areResourcesConflicting) ConflictingResourcesWithRunningObsMode
    else await(sequencerUtil.startSequencers(obsMode, extractSequencers(obsMode)))
  }

  def isOcsAvailable(httpLocation: HttpLocation): Future[Boolean] = SequencerApiFactory.make(httpLocation).isAvailable
  def getRunningObsModes: Future[Set[String]]                     = locationService.listBy(ESW, Sequencer).map(_.map(getObsMode).toSet)
  def getObsMode(akkaLocation: AkkaLocation): String              = akkaLocation.prefix.componentName

  def extractSequencers(observingMode: String): Sequencers = ???
  def extractResources(observingMode: String): Resources   = ???

}
