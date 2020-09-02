package esw.backend.testkit.stubs

import akka.actor.CoordinatedShutdown.UnknownReason
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Subsystem.{ESW, IRIS}
import csw.prefix.models.{Prefix, Subsystem}
import esw.backend.testkit.utils.IOUtils
import esw.ocs.api.models.ObsMode
import esw.ocs.testkit.utils.LocationUtils
import esw.sm.api.SequenceManagerApi
import esw.sm.api.models.{AgentStatus, ProvisionConfig, SequenceComponentStatus}
import esw.sm.api.protocol._
import esw.sm.app.SequenceManagerWiring

import scala.concurrent.Future

class SequenceManagerStubImpl extends SequenceManagerApi {

  override def configure(obsMode: ObsMode): Future[ConfigureResponse] = {
    val componentId = ComponentId(Prefix(ESW, obsMode.name), Sequencer)
    Future.successful(ConfigureResponse.Success(componentId))
  }

  override def provision(config: ProvisionConfig): Future[ProvisionResponse] = Future.successful(ProvisionResponse.Success)

  override def getRunningObsModes: Future[GetRunningObsModesResponse] =
    Future.successful(GetRunningObsModesResponse.Success(Set(ObsMode("darknight"))))

  override def startSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[StartSequencerResponse] =
    Future.successful(StartSequencerResponse.Started(ComponentId(Prefix(subsystem, obsMode.name), Sequencer)))

  override def restartSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[RestartSequencerResponse] =
    Future.successful(RestartSequencerResponse.Success(ComponentId(Prefix(subsystem, obsMode.name), Sequencer)))

  override def shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    Future.successful(ShutdownSequencersResponse.Success)

  override def shutdownSubsystemSequencers(subsystem: Subsystem): Future[ShutdownSequencersResponse] =
    Future.successful(ShutdownSequencersResponse.Success)

  override def shutdownObsModeSequencers(obsMode: ObsMode): Future[ShutdownSequencersResponse] =
    Future.successful(ShutdownSequencersResponse.Success)

  override def shutdownAllSequencers(): Future[ShutdownSequencersResponse] = Future.successful(ShutdownSequencersResponse.Success)

  override def shutdownSequenceComponent(prefix: Prefix): Future[ShutdownSequenceComponentResponse] =
    Future.successful(ShutdownSequenceComponentResponse.Success)

  override def shutdownAllSequenceComponents(): Future[ShutdownSequenceComponentResponse] =
    Future.successful(ShutdownSequenceComponentResponse.Success)

  override def getAgentStatus: Future[AgentStatusResponse] =
    Future.successful(
      AgentStatusResponse.Success(
        List(
          AgentStatus(
            ComponentId(Prefix(IRIS, "Agent"), Machine),
            List(SequenceComponentStatus(ComponentId(Prefix(IRIS, "IRIS_123"), SequenceComponent), None))
          )
        ),
        List(SequenceComponentStatus(ComponentId(Prefix(ESW, "ESW_45"), SequenceComponent), None))
      )
    )
}

class SequenceManagerStub(val locationService: LocationService)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command])
    extends LocationUtils {
  private var seqManagerWiring: Option[SequenceManagerWiring] = _

  def spawnMockSm(): SequenceManagerWiring = {
    val wiring = new SequenceManagerWiring(IOUtils.writeResourceToFile("smObsModeConfig.conf"), true, None) {
      override lazy val actorSystem: ActorSystem[SpawnProtocol.Command] = actorSystem
      override lazy val sequenceManager: SequenceManagerApi             = new SequenceManagerStubImpl()
    }
    seqManagerWiring = Some(wiring)
    wiring.start()
    wiring
  }

  def shutdown(): Unit = seqManagerWiring.foreach(_.shutdown(UnknownReason).futureValue)
}
