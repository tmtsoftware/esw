package esw.examples

import org.apache.pekko.actor.typed.{ActorSystem, SpawnProtocol}
import org.apache.pekko.stream.scaladsl.Source
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.{PekkoConnection, HttpConnection}
import csw.location.api.models.{PekkoLocation, ComponentId, HttpLocation}
import csw.location.api.scaladsl.LocationService
import csw.location.client.ActorSystemFactory
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.params.commands.{CommandName, Sequence, SequenceCommand, Setup}
import csw.params.core.models.Id
import csw.prefix.models.Prefix
import csw.prefix.models.Subsystem.ESW
import csw.time.core.models.UTCTime
import esw.ocs.api.SequencerApi
import esw.ocs.api.actor.client.SequencerApiFactory
import esw.ocs.api.models.StepList
import esw.ocs.api.protocol.SequencerStateResponse
import msocket.api.Subscription

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object SequencerAPIExample {

  // #instantiate-pekko-interface
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "example")
  private val sequencerPekkoConnection: PekkoConnection = PekkoConnection(ComponentId(Prefix(ESW, "IRIS_DARKNIGHT"), Sequencer))
  private val locationService: LocationService          = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  private val sequencerPekkoLocation: PekkoLocation =
    Await.result(locationService.resolve(sequencerPekkoConnection, 10.seconds), 10.seconds).get

  private val sequencer: SequencerApi = SequencerApiFactory.make(sequencerPekkoLocation)
  // #instantiate-pekko-interface

  // #instantiate-http-direct-interface
  private val sequencerHttpConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, "IRIS_DARKNIGHT"), Sequencer))
  private val sequencerHttpLocation: HttpLocation =
    Await.result(locationService.resolve(sequencerHttpConnection, 10.seconds), 10.seconds).get
  private val sequencerHttpClient: SequencerApi = SequencerApiFactory.make(sequencerHttpLocation)

  def main(args: Array[String]): Unit = {
    sequencerHttpClient.getSequence
    // #instantiate-http-direct-interface

    // #add
    val stepsToAdd: List[SequenceCommand] = List(
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-iris")),
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-tcs"))
    )
    sequencer.add(stepsToAdd)
    // #add

    // #prepend
    val stepsToPrepend: List[SequenceCommand] = List(
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-iris")),
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-tcs"))
    )
    sequencer.prepend(stepsToPrepend)
    // #prepend

    // #getSequence
    val stepList: StepList = Await.result(sequencer.getSequence, 1.seconds).get
    // #getSequence

    // #replace
    val stepsToReplace: List[SequenceCommand] = List(
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-iris")),
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-tcs"))
    )
    sequencer.replace(stepList.steps(4).id, stepsToReplace)
    // #replace

    // #insertAfter
    val stepsToInsertAfter: List[SequenceCommand] = List(
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-iris")),
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-tcs"))
    )
    sequencer.insertAfter(stepList.steps(4).id, stepsToInsertAfter)
    // #insertAfter

    // #delete
    val stepToDelete: Id = stepList.steps(4).id
    sequencer.delete(stepToDelete)
    // #delete

    // #addRemoveBreakpoint
    val breakpointStep: Id = stepList.steps(4).id
    sequencer.addBreakpoint(breakpointStep)
    sequencer.removeBreakpoint(breakpointStep)
    // #addRemoveBreakpoint

    // #reset
    sequencer.reset()
    // #reset

    // #pause-resume
    sequencer.pause
    sequencer.resume
    // #pause-resume

    // #getSequenceComponent
    sequencer.getSequenceComponent
    // #getSequenceComponent

    // #isAvailable
    sequencer.isAvailable
    // #isAvailable

    // #online-offline
    sequencer.isOnline
    sequencer.goOnline()
    sequencer.goOffline()
    // #online-offline

    // #abortSequence
    sequencer.abortSequence()
    // #abortSequence

    // #stop
    sequencer.stop()
    // #stop

    // #getSequencerState
    sequencer.getSequencerState
    // #getSequencerState

    // #diagnosticMode
    sequencer.diagnosticMode(UTCTime.now(), "diagnostic-mode")
    // #diagnosticMode

    // #operationsMode
    sequencer.operationsMode()
    // #operationsMode

    // #subscribeSequencerState
    val sequencerStateSource: Source[SequencerStateResponse, Subscription] =
      sequencer.subscribeSequencerState()
    // #subscribeSequencerState

    // #loadSequence
    val sequence: Sequence = Sequence(
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-iris")),
      Setup(Prefix(ESW, "filter.wheel"), CommandName("setup-tcs"))
    )
    sequencer.loadSequence(sequence)
    // #loadSequence

    // #startSequence
    sequencer.startSequence()
    // #startSequence

    // #getSequenceComponent
    sequencer.getSequenceComponent
    // #getSequenceComponent
  }
}
