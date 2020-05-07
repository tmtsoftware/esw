package esw.ocs.testkit.utils

import akka.actor.typed.ActorRef
import csw.command.client.extensions.AkkaLocationExt.RichAkkaLocation
import csw.command.client.messages.sequencer.SequencerMsg
import csw.location.api.models.{AkkaLocation, ComponentType}
import csw.prefix.models.{Prefix, Subsystem}
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.{SequenceComponentImpl, SequencerApiFactory, SequencerImpl}
import esw.ocs.api.protocol.ScriptError
import esw.ocs.app.wiring.{SequenceComponentWiring, SequencerWiring}
import esw.ocs.testkit.simulation.SimulationSequencerWiring

import scala.collection.mutable

trait SequencerUtils extends BaseTestSuite with LocationUtils {

  private val sequenceComponentLocations: mutable.Buffer[AkkaLocation] = mutable.Buffer.empty

  def shutdownAllSequencers(): Unit = {
    sequenceComponentLocations.foreach(new SequenceComponentImpl(_).unloadScript())
    clearSequenceCompBuffer()
  }

  def clearSequenceCompBuffer(): Unit = sequenceComponentLocations.clear()

  def spawnSequencerRef(subsystem: Subsystem, observingMode: String): ActorRef[SequencerMsg] =
    spawnSequencer(subsystem, observingMode).rightValue.sequencerRef

  def spawnSequencerProxy(subsystem: Subsystem, observingMode: String) =
    new SequencerImpl(spawnSequencerRef(subsystem, observingMode))

  def loadScript(seqCompLocation: AkkaLocation, subsystem: Subsystem, observingMode: String): Either[ScriptError, AkkaLocation] =
    new SequenceComponentImpl(seqCompLocation)
      .loadScript(subsystem, observingMode)
      .futureValue
      .response

  def spawnSequencer(subsystem: Subsystem, observingMode: String): Either[ScriptError, AkkaLocation] =
    spawnSequenceComponent(subsystem, None).flatMap(loadScript(_, subsystem, observingMode))

  def spawnSequencerInSimulation(subsystem: Subsystem, observingMode: String): Either[ScriptError, AkkaLocation] =
    spawnSequenceComponentInSimulation(subsystem, None).flatMap(loadScript(_, subsystem, observingMode))

  def spawnSequenceComponent(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring = SequenceComponentWiring.make(subsystem, name, new SequencerWiring(_, _, _).sequencerServer, actorSystem)
    wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
  }

  def sequencerClient(subsystem: Subsystem, observingMode: String): SequencerApi =
    SequencerApiFactory.make(resolveHTTPLocation(Prefix(subsystem, observingMode), ComponentType.Sequencer))

  def spawnSequenceComponentInSimulation(subsystem: Subsystem, name: Option[String]): Either[ScriptError, AkkaLocation] = {
    val wiring =
      SequenceComponentWiring.make(subsystem, name, new SimulationSequencerWiring(_, _, _).sequencerServer, actorSystem)
    wiring.start().map { seqCompLocation =>
      sequenceComponentLocations += seqCompLocation
      seqCompLocation
    }
  }
}
