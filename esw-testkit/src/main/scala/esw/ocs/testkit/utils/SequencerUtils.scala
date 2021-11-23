package esw.ocs.testkit.utils

import akka.actor.typed.ActorRef
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.{AkkaLocation, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory, SequencerImpl}
import esw.ocs.api.models.ObsMode
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{SequencerLocation, Unhandled}
import esw.ocs.app.simulation.SimulationSequencerWiring
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.internal.SequencerServerFactory

import scala.collection.mutable

trait SequencerUtils extends LocationUtils {

  private val sequenceComponentLocations: mutable.Buffer[AkkaLocation] = mutable.Buffer.empty

  def shutdownAllSequencers(): Unit = {
    sequenceComponentLocations.foreach(new SequenceComponentImpl(_).unloadScript())
    sequenceComponentLocations.clear()
  }

  def spawnSequencerRef(sequencerPrefix: Prefix): ActorRef[SequencerMsg] =
    spawnSequencer(sequencerPrefix).sequencerRef

  def spawnSequencerProxy(sequencerPrefix: Prefix) =
    new SequencerImpl(spawnSequencerRef(sequencerPrefix))

  def spawnSequencer(sequencerPrefix: Prefix, agentPrefix: Option[Prefix] = None): AkkaLocation =
    loadScript(spawnSequenceComponent(sequencerPrefix.subsystem, None, agentPrefix), sequencerPrefix)

  def spawnSequencerInSimulation(sequencerPrefix: Prefix, agentPrefix: Option[Prefix] = None): AkkaLocation =
    loadScript(spawnSequenceComponentInSimulation(sequencerPrefix.subsystem, None, agentPrefix), sequencerPrefix)

  def sequencerClient(subsystem: Subsystem, obsMode: ObsMode): SequencerApi =
    SequencerApiFactory.make(resolveHTTPLocation(Prefix(subsystem, obsMode.name), ComponentType.Sequencer))

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String], agentPrefix: Option[Prefix] = None): AkkaLocation =
    spawnSequenceComponent(subsystem, name, agentPrefix, new SequencerWiring(_, _).sequencerServer)

  def spawnSequenceComponentInSimulation(
      subsystem: Subsystem,
      name: Option[String],
      agentPrefix: Option[Prefix] = None
  ): AkkaLocation =
    spawnSequenceComponent(subsystem, name, agentPrefix, new SimulationSequencerWiring(_, _).sequencerServer)

  private def spawnSequenceComponent(
      subsystem: Subsystem,
      name: Option[String],
      agentPrefix: Option[Prefix],
      sequencerFactory: SequencerServerFactory
  ) = {
    val wiring = SequenceComponentWiring.make(subsystem, name, agentPrefix, sequencerFactory, actorSystem)
    val loc = wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
    loc.rightValue
  }

  def loadScript(seqCompLocation: AkkaLocation, sequencerPrefix: Prefix): AkkaLocation =
    new SequenceComponentImpl(seqCompLocation).loadScript(sequencerPrefix).futureValue match {
      case SequencerLocation(location) => location
      case error: ScriptError          => throw new RuntimeException(s"failed to load script: ${error.msg}")
      case Unhandled(_, _, msg)        => throw new RuntimeException(s"failed to load script: $msg")
    }

}
