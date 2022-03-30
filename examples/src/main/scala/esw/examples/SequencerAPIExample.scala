/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.examples

import akka.actor.typed.{ActorSystem, SpawnProtocol}
import akka.stream.scaladsl.Source
import csw.location.api.models.ComponentType.Sequencer
import csw.location.api.models.Connection.{AkkaConnection, HttpConnection}
import csw.location.api.models.{AkkaLocation, ComponentId, HttpLocation}
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

object SequencerAPIExample extends App {

  // #instantiate-akka-interface
  private implicit val actorSystem: ActorSystem[SpawnProtocol.Command] = ActorSystemFactory.remote(SpawnProtocol(), "example")
  private val sequencerAkkaConnection: AkkaConnection = AkkaConnection(ComponentId(Prefix(ESW, "IRIS_DARKNIGHT"), Sequencer))
  private val locationService: LocationService        = HttpLocationServiceFactory.makeLocalClient(actorSystem)

  private val sequencerAkkaLocation: AkkaLocation =
    Await.result(locationService.resolve(sequencerAkkaConnection, 10.seconds), 10.seconds).get

  private val sequencer: SequencerApi = SequencerApiFactory.make(sequencerAkkaLocation)
  // #instantiate-akka-interface

  // #instantiate-http-direct-interface
  private val sequencerHttpConnection: HttpConnection = HttpConnection(ComponentId(Prefix(ESW, "IRIS_DARKNIGHT"), Sequencer))
  private val sequencerHttpLocation: HttpLocation =
    Await.result(locationService.resolve(sequencerHttpConnection, 10.seconds), 10.seconds).get
  private val sequencerHttpClient: SequencerApi = SequencerApiFactory.make(sequencerHttpLocation)

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
  private val stepList: StepList = Await.result(sequencer.getSequence, 1.seconds).get
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
  private val stepToDelete: Id = stepList.steps(4).id
  sequencer.delete(stepToDelete)
  // #delete

  // #addRemoveBreakpoint
  private val breakpointStep: Id = stepList.steps(4).id
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
