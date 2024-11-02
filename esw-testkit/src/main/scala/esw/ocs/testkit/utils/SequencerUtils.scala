package esw.ocs.testkit.utils

import org.apache.pekko.actor.typed.ActorRef
import csw.command.client.extensions.PekkoLocationExt.RichPekkoLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.{PekkoLocation, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory, SequencerImpl}
import esw.ocs.api.models.{ObsMode, Variation}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.api.protocol.SequenceComponentResponse.{SequencerLocation, Unhandled}
import esw.ocs.app.simulation.SimulationSequencerWiring
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.impl.internal.SequencerServerFactory
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{Await, Future}

import scala.collection.mutable

trait SequencerUtils extends LocationUtils {

  private val sequenceComponentLocations: mutable.Buffer[PekkoLocation] = mutable.Buffer.empty

  def shutdownAllSequencers(): Unit = {
    val fList = sequenceComponentLocations.map(new SequenceComponentImpl(_).unloadScript()).toList
    Await.ready(Future.sequence(fList), 100.seconds)
    sequenceComponentLocations.clear()
  }

  def spawnSequencerRef(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation] = None): ActorRef[SequencerMsg] =
    spawnSequencer(subsystem, obsMode, variation).sequencerRef

  def spawnSequencerProxy(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation] = None) =
    new SequencerImpl(spawnSequencerRef(subsystem, obsMode, variation))

  def spawnSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation] = None,
      agentPrefix: Option[Prefix] = None
  ): PekkoLocation =
    loadScript(spawnSequenceComponent(subsystem, None, agentPrefix), subsystem, obsMode, variation)

  def spawnSequencerInSimulation(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation] = None,
      agentPrefix: Option[Prefix] = None
  ): PekkoLocation =
    loadScript(spawnSequenceComponentInSimulation(subsystem, None, agentPrefix), subsystem, obsMode, variation)

  def sequencerClient(subsystem: Subsystem, obsMode: ObsMode, variation: Option[Variation] = None): SequencerApi = {
    SequencerApiFactory.make(resolveHTTPLocation(Variation.prefix(subsystem, obsMode, variation), ComponentType.Sequencer))
  }

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String], agentPrefix: Option[Prefix] = None): PekkoLocation =
    spawnSequenceComponent(
      subsystem,
      name,
      agentPrefix,
      new SequencerWiring(_, _).sequencerServer
    )

  private def spawnSequenceComponentInSimulation(
      subsystem: Subsystem,
      name: Option[String],
      agentPrefix: Option[Prefix] = None
  ): PekkoLocation =
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

  def loadScript(
      seqCompLocation: PekkoLocation,
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation] = None
  ): PekkoLocation =
    new SequenceComponentImpl(seqCompLocation).loadScript(subsystem, obsMode, variation).futureValue match {
      case SequencerLocation(location) => location
      case error: ScriptError          => throw new RuntimeException(s"failed to load script: ${error.msg}")
      case Unhandled(_, _, msg)        => throw new RuntimeException(s"failed to load script: $msg")
    }

}
