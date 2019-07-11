package esw.ocs.framework.core

import akka.actor.testkit.typed.scaladsl.{ActorTestKitBase, TestProbe}
import csw.params.commands.CommandResponse.{Completed, SubmitResponse}
import csw.params.commands.{CommandName, Setup}
import csw.params.core.models.{Id, Prefix}
import esw.ocs.framework.BaseTestSuite
import esw.ocs.framework.api.models.messages.ProcessSequenceError
import esw.ocs.framework.api.models.messages.SequencerMsg.{Available, GetSequence, ProcessSequence}
import esw.ocs.framework.api.models.{Sequence, StepList}
import esw.ocs.framework.dsl.ScriptDsl
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.Future

class SequencerBehaviorTest extends ActorTestKitBase with BaseTestSuite with MockitoSugar {

  "SequencerBehavior" must {

    val sequencer = mock[Sequencer]
    val scriptDsl = mock[ScriptDsl]
    val sequencerActor =
      system.systemActorOf(SequencerBehavior.behavior(sequencer, scriptDsl), "sequencer-actor-system").futureValue

    "processSequence" in {
      val testProbe: TestProbe[Either[ProcessSequenceError, SubmitResponse]] = TestProbe()

      val command1 = Setup(Prefix("test"), CommandName("command-1"), None)
      val sequence = Sequence(Id(), Seq(command1))

      val processResponse: Either[ProcessSequenceError, SubmitResponse] = Right(Completed(command1.runId))

      when(sequencer.processSequence(sequence)).thenReturn(Future.successful(processResponse))

      sequencerActor ! ProcessSequence(sequence, testProbe.ref)
      testProbe.expectMessage(processResponse)
    }

    "Available" in {
      val testProbe: TestProbe[Boolean] = TestProbe()
      val availableResponse             = true

      when(sequencer.isAvailable).thenReturn(Future.successful(availableResponse))

      sequencerActor ! Available(testProbe.ref)
      testProbe.expectMessage(availableResponse)
    }

    "GetSequence" in {
      val testProbe: TestProbe[StepList] = TestProbe()

      val stepListResponse = StepList.empty
      when(sequencer.getSequence).thenReturn(Future.successful(stepListResponse))

      sequencerActor ! GetSequence(testProbe.ref)
      testProbe.expectMessage(stepListResponse)
    }

  }
}
