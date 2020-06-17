package esw.ocs.testkit.utils

import akka.actor.typed.ActorRef
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.{AkkaLocation, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory, SequencerImpl}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{SequencerLocation, Unhandled}
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.internal.SequencerServerFactory
import esw.ocs.testkit.simulation.SimulationSequencerWiring

import scala.collection.mutable

trait SequencerUtils extends LocationUtils {

  private val sequenceComponentLocations: mutable.Buffer[AkkaLocation] = mutable.Buffer.empty

  def shutdownAllSequencers(): Unit = {
    sequenceComponentLocations.foreach(new SequenceComponentImpl(_).unloadScript())
    sequenceComponentLocations.clear()
  }

  def spawnSequencerRef(subsystem: Subsystem, observingMode: String): ActorRef[SequencerMsg] =
    spawnSequencer(subsystem, observingMode).sequencerRef

  def spawnSequencerProxy(subsystem: Subsystem, observingMode: String): SequencerApi =
    new SequencerImpl(spawnSequencerRef(subsystem, observingMode))

  def spawnSequencer(subsystem: Subsystem, observingMode: String): AkkaLocation =
    loadScript(spawnSequenceComponent(subsystem, None), subsystem, observingMode)

  def spawnSequencerInSimulation(subsystem: Subsystem, observingMode: String): AkkaLocation =
    loadScript(spawnSequenceComponentInSimulation(subsystem, None), subsystem, observingMode)

  def sequencerClient(subsystem: Subsystem, observingMode: String): SequencerApi =
    SequencerApiFactory.make(resolveHTTPLocation(Prefix(subsystem, observingMode), ComponentType.Sequencer))

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String]): AkkaLocation =
    spawnSequenceComponent(subsystem, name, new SequencerWiring(_, _, _).sequencerServer)

  def spawnSequenceComponentInSimulation(subsystem: Subsystem, name: Option[String]): AkkaLocation =
    spawnSequenceComponent(subsystem, name, new SimulationSequencerWiring(_, _, _).sequencerServer)

  private def spawnSequenceComponent(subsystem: Subsystem, name: Option[String], sequencerFactory: SequencerServerFactory) = {
    val wiring = SequenceComponentWiring.make(subsystem, name, sequencerFactory, actorSystem)
    val loc = wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
    loc.rightValue
  }

  private def loadScript(seqCompLocation: AkkaLocation, subsystem: Subsystem, observingMode: String) =
    new SequenceComponentImpl(seqCompLocation).loadScript(subsystem, observingMode).futureValue match {
      case SequencerLocation(location) => location
      case error: ScriptError          => throw new RuntimeException(s"failed to load script: ${error.msg}")
      case Unhandled(_, _, msg)        => throw new RuntimeException(s"failed to load script: $msg")
    }

}
