package esw.ocs.core

import akka.Done
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.actor.typed.{ActorRef, ActorSystem, SpawnProtocol}
import akka.util.Timeout
import csw.command.client.messages.{ProcessSequence, ProcessSequenceResponse, SequencerMsg}
import csw.location.api.extensions.URIExtension.RichURI
import csw.location.api.scaladsl.LocationService
import csw.location.client.scaladsl.HttpLocationServiceFactory
import csw.location.models.Connection.AkkaConnection
import csw.location.models.{ComponentId, ComponentType}
import csw.params.commands.CommandResponse.Completed
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.Prefix
import csw.testkit.scaladsl.ScalaTestFrameworkTestKit
import esw.ocs.BaseTestSuite
import esw.ocs.api.models.Step
import esw.ocs.api.models.StepStatus.Finished
import esw.ocs.api.models.messages.EditorResponse
import esw.ocs.api.models.messages.SequencerMessages.{Add, GetSequence}
import esw.ocs.internal.SequencerWiring

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong

class SequencerTest extends ScalaTestFrameworkTestKit with BaseTestSuite {

  import frameworkTestKit._
  private implicit val sys: ActorSystem[SpawnProtocol] = actorSystem

  private implicit val askTimeout: Timeout  = Timeout(5.seconds)
  private implicit val scheduler: Scheduler = actorSystem.scheduler

  private val sequencerId   = "testSequencerId1"
  private val observingMode = "testObservingMode1"

  private var locationService: LocationService  = _
  private var wiring: SequencerWiring           = _
  private var sequencer: ActorRef[SequencerMsg] = _

  override def beforeAll(): Unit = {
    super.beforeAll()
    locationService = HttpLocationServiceFactory.makeLocalClient
  }

  override protected def beforeEach(): Unit = {
    wiring = new SequencerWiring("testSequencerId1", "testObservingMode1")
    wiring.start()
    sequencer = resolveSequencer()
  }

  override protected def afterEach(): Unit = {
    wiring.shutDown()
  }

  "Sequencer" must {
    "process a given sequence | ESW-145" in {
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(command3)

      val processSeqResponse: Future[ProcessSequenceResponse] = sequencer ? (ProcessSequence(sequence, _))

      processSeqResponse.futureValue.response.rightValue should ===(Completed(sequence.runId))

      (sequencer ? GetSequence).futureValue.steps should ===(
        List(Step(command3, Finished.Success(Completed(command3.runId)), hasBreakpoint = false))
      )
    }

    "process sequence and execute commands that are added later | ESW-145" in {
      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val command2 = Setup(Prefix("test"), CommandName("command-2"), None)
      val command3 = Setup(Prefix("test"), CommandName("command-3"), None)
      val sequence = Sequence(command1, command2)

      val processSeqResponse: Future[ProcessSequenceResponse] = sequencer ? (ProcessSequence(sequence, _))

      val addResponse: Future[EditorResponse] = sequencer ? (Add(List(command3), _))
      addResponse.futureValue.response.rightValue should ===(Done)

      processSeqResponse.futureValue.response.rightValue should ===(Completed(sequence.runId))

      (sequencer ? GetSequence).futureValue.steps should ===(
        List(
          Step(command1, Finished.Success(Completed(command1.runId)), hasBreakpoint = false),
          Step(command2, Finished.Success(Completed(command2.runId)), hasBreakpoint = false),
          Step(command3, Finished.Success(Completed(command3.runId)), hasBreakpoint = false)
        )
      )
    }
  }

  private def resolveSequencer(): ActorRef[SequencerMsg] =
    locationService
      .resolve(AkkaConnection(ComponentId(s"$sequencerId@$observingMode", ComponentType.Sequencer)), 5.seconds)
      .futureValue
      .value
      .uri
      .toActorRef
      .unsafeUpcast[SequencerMsg]
}
