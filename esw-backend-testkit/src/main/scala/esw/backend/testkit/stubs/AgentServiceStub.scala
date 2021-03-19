package esw.backend.testkit.stubs

import java.net.URI
import java.nio.file.Path

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import csw.backend.auth.MockedAuth
import csw.location.api.models.ComponentType.{Machine, SequenceComponent, Sequencer}
import csw.location.api.models.Connection.AkkaConnection
import csw.location.api.models.{AkkaLocation, ComponentId, Metadata}
import csw.location.api.scaladsl.LocationService
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import esw.agent.service.api.AgentServiceApi
import esw.agent.service.api.models._
import esw.agent.service.app.AgentServiceWiring
import esw.ocs.testkit.utils.LocationUtils

import scala.concurrent.Future

class AgentServiceStubImpl extends AgentServiceApi {
  override def spawnSequenceManager(
      agentPrefix: Prefix,
      obsModeConfigPath: Path,
      isConfigLocal: Boolean,
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def spawnSequenceComponent(
      agentPrefix: Prefix,
      componentName: String,
      version: Option[String]
  ): Future[SpawnResponse] = Future.successful(Spawned)

  override def spawnContainers(
      agentPrefix: Prefix,
      hostConfigPath: String,
      isConfigLocal: Boolean
  ): Future[SpawnContainersResponse] = Future.successful(Completed(Map.empty))

  override def killComponent(componentId: ComponentId): Future[KillResponse] = Future.successful(Killed)

  override def getAgentStatus: Future[AgentStatusResponse] = {
    val sequenceComponentStatus = SequenceComponentStatus(
      ComponentId(Prefix(ESW, "seqcomp1"), SequenceComponent),
      Some(
        AkkaLocation(
          AkkaConnection(ComponentId(Prefix(ESW, "IRIS_DARKNIGHT"), Sequencer)),
          new URI(""),
          Metadata().withSequenceComponentPrefix(Prefix(ESW, "seqcomp1"))
        )
      )
    )
    Future.successful(
      AgentStatusResponse.Success(
        List(AgentStatus(ComponentId(Prefix(ESW, "agent1"), Machine), List(sequenceComponentStatus))),
        List.empty
      )
    )
  }
}

class AgentServiceStub(val locationService: LocationService)(implicit val actorSystem: ActorSystem[SpawnProtocol.Command])
    extends LocationUtils {
  private var agentServiceWiring: Option[AgentServiceWiring] = _
  def spawnMockAgentService(): AgentServiceWiring = {
    println("hello")
    val wiring: AgentServiceWiring = new AgentServiceWiring() {
      override lazy val agentActorSystem: ActorSystem[SpawnProtocol.Command] = actorSystem
      override lazy val agentService: AgentServiceApi                        = new AgentServiceStubImpl()
      private val mockedAuth                                                 = new MockedAuth
      override private[esw] lazy val securityDirective                       = mockedAuth._securityDirectives
    }
    agentServiceWiring = Some(wiring)
    wiring.start()
    wiring
  }

  def shutdown(): Unit = {
    agentServiceWiring.foreach(_.stop().futureValue)
  }
}
