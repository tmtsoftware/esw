package esw.sm.app

import java.nio.file.{Path, Paths}

import akka.Done
import akka.actor.CoordinatedShutdown
import akka.actor.typed.{ActorSystem, SpawnProtocol}
import com.typesafe.config.ConfigFactory
import csw.aas.http.SecurityDirectives
import csw.location.api.models.ComponentId
import csw.location.api.models.ComponentType.Service
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.client.ActorSystemFactory
import csw.prefix.models.Prefix
import esw.ocs.app.SequencerApp
import esw.ocs.app.SequencerAppCommand.SequenceComponent
import esw.ocs.app.wiring.SequenceComponentWiring
import esw.ocs.testkit.EswTestKit
import esw.sm.api.SequenceManagerApi
import esw.sm.api.actor.client.SequenceManagerApiFactory

import scala.collection.mutable.ArrayBuffer

object TestSetup extends EswTestKit {
  private val seqCompWirings    = ArrayBuffer.empty[SequenceComponentWiring]
  private val seqManagerWirings = ArrayBuffer.empty[SequenceManagerWiring]

  // Setup Sequence components for subsystems
  def startSequenceComponents(prefixes: Prefix*): Unit =
    prefixes.foreach { prefix =>
      seqCompWirings += SequencerApp.run(SequenceComponent(prefix.subsystem, Some(prefix.componentName), None))
    }

  val obsModeConfigPath: Path = Paths.get(ClassLoader.getSystemResource("smObsModeConfig.conf").toURI)

  def startSequenceManagerAuthEnabled(
      prefix: Prefix,
      tokenFactory: () => Option[String],
      obsModeConfigPath: Path = obsModeConfigPath,
      isConfigLocal: Boolean = true,
      agentPrefix: Option[Prefix] = None,
      simulation: Boolean = false
  ): SequenceManagerApi =
    startSequenceManager(prefix, obsModeConfigPath, isConfigLocal, simulation, agentPrefix, authDisabled = false, tokenFactory)

  def startSequenceManager(
      prefix: Prefix,
      obsModeConfigPath: Path = obsModeConfigPath,
      isConfigLocal: Boolean = true,
      agentPrefix: Option[Prefix] = None,
      simulation: Boolean = false
  ): SequenceManagerApi =
    startSequenceManager(prefix, obsModeConfigPath, isConfigLocal, simulation, agentPrefix, authDisabled = true, () => None)

  private def startSequenceManager(
      prefix: Prefix,
      obsModeConfig: Path,
      isConfigLocal: Boolean,
      simulation: Boolean,
      agentPrefix: Option[Prefix],
      authDisabled: Boolean,
      tokenFactory: () => Option[String]
  ): SequenceManagerApi = {
    val authConfigS = """
      |auth-config {
      |  realm = TMT
      |  client-id = tmt-backend-app
      |}""".stripMargin

    val _system: ActorSystem[SpawnProtocol.Command] =
      ActorSystemFactory.remote(SpawnProtocol(), "sequence-manager")
    val config = ConfigFactory.parseString(authConfigS).withFallback(_system.settings.config)

    val authConfig =
      if (authDisabled) ConfigFactory.parseString("auth-config.disabled=true").withFallback(config)
      else config

    val securityDirectives = SecurityDirectives(authConfig, locationService)
    val wiring =
      SequenceManagerWiring(None, obsModeConfig, isConfigLocal, agentPrefix, _system, securityDirectives, simulation)
    wiring.start()
    seqManagerWirings += wiring
    val smLocation = resolveHTTPLocation(prefix, Service)
    SequenceManagerApiFactory.makeHttpClient(smLocation, tokenFactory)
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
