package esw.backend.testkit

import java.nio.file.Path

import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import caseapp.RemainingArgs
import com.typesafe.config.ConfigFactory
import csw.location.api.scaladsl.LocationService
import csw.logging.client.scaladsl.LoggingSystemFactory
import csw.network.utils.Networks
import csw.testkit.scaladsl.CSWService.AlarmServer
import esw.backend.testkit.TSServicesCommands._
import esw.backend.testkit.stubs.{AgentServiceStub, GatewayStub, SequenceManagerStub}
import esw.commons.cli.EswCommandApp
import esw.ocs.testkit.Service.{AAS, AgentService, Gateway, SequenceManager, WrappedCSWService}
import esw.ocs.testkit.{EswTestKit, Service}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture

import scala.util.control.NonFatal

object BackendService extends EswCommandApp[TSServicesCommands] {
  override def appName: String  = getClass.getSimpleName.dropRight(1) // remove $ from class name
  override def progName: String = "backend-testkit"

  override def run(options: TSServicesCommands, remainingArgs: RemainingArgs): Unit =
    options match {
      case Start(services, commandRoles, alarmConf) => run(services, commandRoles, alarmConf)
    }

  private def run(services: List[Service], commandRoles: Path, alarmConf: String): Unit = {
    val servicesWithoutGatewayAndAgent = services.filterNot(x => x == Gateway || x == AgentService || x == AAS)
    val eswTestKit: EswTestKit         = new EswTestKit(servicesWithoutGatewayAndAgent: _*) {}

    var gatewayWiring: Option[GatewayStub]                       = None
    var sequenceManagerWiring: Option[SequenceManagerStub]       = None
    var agentServiceWiring: Option[AgentServiceStub]             = None
    val locationService: LocationService                         = eswTestKit.locationService
    implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = eswTestKit.actorSystem

    def shutdown(): Unit = {
      gatewayWiring.foreach(_.shutdownGateway())
      sequenceManagerWiring.foreach(_.shutdown())
      agentServiceWiring.foreach(_.shutdown())
      eswTestKit.shutdownAgent()
      eswTestKit.afterAll()
    }

    try {
      LoggingSystemFactory.start(progName, "0.1.0-SNAPSHOT", Networks().hostname, actorSystem)
      import eswTestKit.frameworkTestKit.frameworkWiring.alarmServiceFactory

      def initDefaultAlarms() = {
        val config            = ConfigFactory.parseResources(alarmConf)
        val alarmAdminService = alarmServiceFactory.makeAdminApi(locationService)(actorSystem)
        alarmAdminService.initAlarms(config, reset = true).futureValue
      }

      eswTestKit.beforeAll()
      if (services.contains(WrappedCSWService(AlarmServer))) initDefaultAlarms()
      if (services.contains(Gateway)) {
        val gateway = new GatewayStub(locationService)
        gatewayWiring = Some(gateway)
        gateway.spawnMockGateway(services.contains(AAS), commandRoles)
      }
      if (services.contains(SequenceManager)) {
        val sequenceManagerStub = new SequenceManagerStub(locationService)
        sequenceManagerWiring = Some(sequenceManagerStub)
        sequenceManagerStub.spawnMockSm()
      }
      if (services.contains(AgentService)) {
        val agentServiceStub = new AgentServiceStub(locationService)
        agentServiceWiring = Some(agentServiceStub)
        agentServiceStub.spawnMockAgentService()
      }
      CoordinatedShutdown(actorSystem).addJvmShutdownHook(shutdown())
    }
    catch {
      case NonFatal(e) => shutdown(); throw e
    }
  }

}
