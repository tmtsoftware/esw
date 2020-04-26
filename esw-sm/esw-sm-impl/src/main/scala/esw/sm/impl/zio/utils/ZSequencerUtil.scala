package esw.sm.impl.zio.utils

import akka.Done
import akka.actor.typed.ActorSystem
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation, Location}
import csw.prefix.models.Subsystem.ESW
import csw.prefix.models.{Prefix, Subsystem}
import esw.commons.Timeouts
import esw.commons.utils.location.EswLocationError
import esw.commons.utils.location.EswLocationError.{RegistrationListingFailed, ResolveLocationFailed}
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.{SequenceComponentApi, SequencerApi}
import esw.sm.api.actor.messages.ConfigureResponse
import esw.sm.api.actor.messages.ConfigureResponse.{ConfigurationFailure, Success}
import esw.sm.api.models.SequenceManagerError.{LocationServiceError, SequencerNotIdle}
import esw.sm.api.models.{SequenceManagerError, SequencerError, Sequencers}
import esw.zio.commons.ZLocationService
import zio._

class ZSequencerUtil(ZLocationService: ZLocationService, ZSequenceComp: ZSequenceComponentUtil)(
    implicit actorSystem: ActorSystem[_]
) {
  private val retryCount = 3

  private def masterSequencerConnection(obsMode: String) = HttpConnection(ComponentId(Prefix(ESW, obsMode), Sequencer))

  def resolveMasterSequencerOf(observingMode: String): IO[LocationServiceError, HttpLocation] =
    ZLocationService
      .resolve(masterSequencerConnection(observingMode), Timeouts.DefaultTimeout)
      .mapError(e => LocationServiceError(e.msg))

  def startSequencers(observingMode: String, requiredSequencers: Sequencers): UIO[ConfigureResponse] =
    ZIO
      .foreachPar(requiredSequencers.subsystems)(startSequencer(_, observingMode, retryCount))
      .flatMap(_ => resolveMasterSequencerOf(observingMode).map(Success))
      .catchAll(e => ZIO.succeed(ConfigurationFailure(e.msg)))

  def checkForSequencersAvailability(sequencers: Sequencers, obsMode: String): IO[SequencerError, Done] =
    ZIO
      .foreach(sequencers.subsystems)(resolveAndCheckAvailability(obsMode, _))
      .orElseFail(LocationServiceError("Failed to check availability of sequencers"))
      .flatMap(b => if (b.contains(false)) ZIO.fail(SequencerNotIdle(obsMode)) else ZIO.succeed(Done))

  def stopSequencers(sequencers: Sequencers, obsMode: String): IO[RegistrationListingFailed, Done] =
    ZIO
      .foreach(sequencers.subsystems)(resolveSequencer(obsMode, _))
      .map(_.map(stopSequencer))
      .as(Done)
      .catchAll {
        case _: ResolveLocationFailed     => ZIO.succeed(Done)
        case e: RegistrationListingFailed => ZIO.fail(e)
      }

  private def getSeqComp(loc: AkkaLocation)                               = ZIO.fromFuture(_ => createSequencerClient(loc).getSequenceComponent)
  private def stopSequencer(loc: AkkaLocation): Task[Done]                = getSeqComp(loc).flatMap(ZSequenceComp.unloadScript)
  private[sm] def createSequencerClient(location: Location): SequencerApi = SequencerApiFactory.make(location)
  private def resolveSequencer(obsMode: String, subsystem: Subsystem)     = ZLocationService.resolveSequencer(subsystem, obsMode)
  private def isSequencerAvailable(seqLoc: AkkaLocation)                  = ZIO.fromFuture(_ => createSequencerClient(seqLoc).isAvailable)
  private def resolveAndCheckAvailability(obsMode: String, subsystem: Subsystem): IO[EswLocationError, Boolean] =
    resolveSequencer(obsMode, subsystem).flatMap(isSequencerAvailable(_).mapError(e => ResolveLocationFailed(e.getMessage)))

  private def loadScript(subSystem: Subsystem, observingMode: String, seqCompApi: SequenceComponentApi) =
    ZIO
      .fromFuture(_ => seqCompApi.loadScript(subSystem, observingMode))
      .mapError(e => SequenceManagerError.LoadScriptError(e.getMessage))
      .flatMap(r => ZIO.fromEither(r.response).mapError(e => SequenceManagerError.LoadScriptError(e.msg)))

  // spawn the sequencer on available SequenceComponent
  private def startSequencer(subSystem: Subsystem, observingMode: String, retryCount: Int): IO[SequencerError, AkkaLocation] =
    ZSequenceComp
      .getAvailableSequenceComponent(subSystem)
      .flatMap(loadScript(subSystem, observingMode, _))
      .retry(Schedule.recurs(retryCount))

}
