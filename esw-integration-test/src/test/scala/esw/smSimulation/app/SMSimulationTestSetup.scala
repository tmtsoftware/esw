package esw.smSimulation.app

import java.nio.file.{Path, Paths}

import akka.Done
import akka.actor.CoordinatedShutdown
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.prefix.models.Prefix
import esw.ocs.app.SequencerApp
import esw.ocs.app.SequencerAppCommand.SequenceComponent
import esw.ocs.app.wiring.SequenceComponentWiring
import esw.ocs.testkit.EswTestKit
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory
import esw.sm.app.SequenceManagerWiring

import scala.collection.mutable.ArrayBuffer

object SMSimulationTestSetup extends EswTestKit {
  private val seqCompWirings    = ArrayBuffer.empty[SequenceComponentWiring]
  private val seqManagerWirings = ArrayBuffer.empty[SequenceManagerWiring]
  val obsModeConfigPath: Path   = Paths.get(ClassLoader.getSystemResource("smSimulationObsModeConfig.conf").toURI)

  def startSequenceComponents(prefixes: Prefix*): Unit =
    prefixes.foreach { prefix =>
      seqCompWirings += SequencerApp.run(SequenceComponent(prefix.subsystem, Some(prefix.componentName), None))
    }

  def startSequenceManagerSimulation(
      prefix: Prefix,
      obsModeConfig: Path,
      isConfigLocal: Boolean,
      agentPrefix: Option[Prefix]
  ): SequenceManagerApi = {
    val simulationWiring = new SequenceManagerSimulationWiring(obsModeConfig, isConfigLocal, agentPrefix)
    simulationWiring.startSimulation()
    seqManagerWirings += simulationWiring
    val smLocation = resolveHTTPLocation(prefix, Service)
    SequenceManagerApiFactory.makeHttpClient(smLocation, () => None)
  }

  def getSMClient(
      prefix: Prefix
  ): SequenceManagerApi = {
    val smLocation = resolveHTTPLocation(prefix, Service)
    SequenceManagerApiFactory.makeHttpClient(smLocation, () => None)
  }

  def unregisterSequenceManager(prefix: Prefix): Done = {
    seqManagerWirings.clear()
    locationService.unregister(AkkaConnection(ComponentId(prefix, Service))).futureValue
    locationService.unregister(HttpConnection(ComponentId(prefix, Service))).futureValue
  }

  def cleanup(): Unit = {
    seqCompWirings.foreach(_.actorRuntime.shutdown(CoordinatedShutdown.JvmExitReason).futureValue)
    seqManagerWirings.foreach(_.shutdown(CoordinatedShutdown.JvmExitReason).futureValue)
    seqCompWirings.clear()
    seqManagerWirings.clear()
  }
}
