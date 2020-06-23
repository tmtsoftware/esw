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
      seqCompWirings += SequencerApp.run(SequenceComponent(prefix.subsystem, Some(prefix.componentName)))
    }

  val path: Path = Paths.get(ClassLoader.getSystemResource("smResources.conf").toURI)

  def startSequenceManager(prefix: Prefix, configFilePath: Path = path): SequenceManagerApi = {
    val authConfigS = """
      |auth-config { 
      |  disabled = true
      |  realm = TMT-test
      |  client-id = esw-sequence-manager
      |}""".stripMargin
    val authConfig  = ConfigFactory.parseString(authConfigS)

    val _system: ActorSystem[SpawnProtocol.Command] =
      ActorSystemFactory.remote(SpawnProtocol(), "sequencer-manager")
    val securityDirectives = SecurityDirectives.authDisabled(authConfig)

    val wiring = SequenceManagerWiring(configFilePath, _system, securityDirectives)
    wiring.start()
    seqManagerWirings += wiring
    val smLocation = resolveHTTPLocation(prefix, Service)
    SequenceManagerApiFactory.make(smLocation)
  }

  def unregisterSequenceManager(prefix: Prefix): Done = {
    seqManagerWirings.clear()
    locationService.unregister(AkkaConnection(ComponentId(prefix, Service))).futureValue
    locationService.unregister(HttpConnection(ComponentId(prefix, Service))).futureValue
  }

  def cleanup(): Unit = {
    seqCompWirings.foreach(_.cswWiring.actorRuntime.shutdown(CoordinatedShutdown.JvmExitReason).futureValue)
    seqManagerWirings.foreach(_.shutdown(CoordinatedShutdown.JvmExitReason).futureValue)
    seqCompWirings.clear()
    seqManagerWirings.clear()
  }
}
