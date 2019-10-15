package esw.ocs.impl.core

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import csw.params.commands.CommandResponse.Error
import csw.params.commands.SequenceCommand
import csw.params.core.models.Id
import esw.ocs.api.BaseTestSuite
import esw.ocs.api.models.Step
import esw.ocs.api.protocol.{Ok, PullNextResult}
import esw.ocs.dsl.script.{ScriptDsl, SequenceOperator}

import scala.concurrent.Future

class EngineTest extends BaseTestSuite {
  private implicit val test: ActorSystem = ActorSystem("test")
  private implicit val mat: Materializer = ActorMaterializer()

  private class Mocks {
    val sequenceOperator: SequenceOperator = mock[SequenceOperator]
    val script: ScriptDsl                  = mock[ScriptDsl]
    val cmd: SequenceCommand               = mock[SequenceCommand]
    val id: Id                             = mock[Id]
  }

  "Engine" must {
    "execute script with available commands" in {
      val mocks = new Mocks
      import mocks._

      val step1 = mock[Step]
      when(step1.command).thenReturn(cmd)
      when(step1.id).thenReturn(id)

      val step2 = mock[Step]
      when(step2.command).thenReturn(cmd)
      when(step2.id).thenReturn(id)

      val engine = new Engine(script)
      when(sequenceOperator.pullNext)
        .thenReturn(Future.successful(PullNextResult(step1)), Future.successful(PullNextResult(step2)))
      when(sequenceOperator.readyToExecuteNext).thenReturn(Future.successful(Ok))
      engine.start(sequenceOperator)
      eventually(verify(script).execute(step1.command))
      eventually(verify(script).execute(step2.command))
    }

    "update command response with Error when script's execute call throws exception" in {
      val mocks = new Mocks
      import mocks._

      val errorMsg = "error message"
      val step     = mock[Step]
      when(step.command).thenReturn(cmd)
      when(step.id).thenReturn(id)

      val engine = new Engine(script)
      when(sequenceOperator.pullNext).thenReturn(Future.successful(PullNextResult(step)))
      when(script.execute(step.command)).thenReturn(Future.failed(new RuntimeException(errorMsg)))
      engine.start(sequenceOperator)
      eventually(verify(sequenceOperator).update(Error(step.id, errorMsg)))
    }
  }
}
