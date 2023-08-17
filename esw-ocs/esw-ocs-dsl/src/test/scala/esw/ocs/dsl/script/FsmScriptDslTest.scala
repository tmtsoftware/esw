package esw.ocs.dsl.script

import org.apache.pekko.Done
import csw.logging.api.javadsl.ILogger
import csw.params.commands.SequenceCommand
import csw.time.core.models.UTCTime
import esw.ocs.dsl.params.Params
import esw.ocs.impl.core.SequenceOperator
import esw.testcommons.BaseTestSuite

import scala.concurrent.Future
import org.mockito.Mockito.{verify, when}
class FsmScriptDslTest extends BaseTestSuite {
  private val strandEc           = StrandEc()
  private val seqOperatorFactory = () => mock[SequenceOperator]
  private val STARTED_STATE      = "STARTED"
  private val params             = Params()
  private val logger             = mock[ILogger]

  override protected def afterAll(): Unit = strandEc.shutdown()

  "become" must {
    "call transition method defined on FsmScriptState and update its internal state | ESW-252" in {
      val initialState           = mock[FsmScriptState]
      val updatedState           = mock[FsmScriptState]
      val shutdownTask: Runnable = () => ()
      when(initialState.transition(STARTED_STATE, params)).thenReturn(updatedState)

      val scriptDsl = new FsmScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask, initialState)
      scriptDsl.become(STARTED_STATE, params)

      verify(initialState).transition(STARTED_STATE, params)
      scriptDsl.getState should ===(updatedState)
    }
  }

  "add" must {
    "call add method defined on FsmScriptState and update its internal state" in {
      val initialState           = mock[FsmScriptState]
      val updatedState           = mock[FsmScriptState]
      val handler                = (_: Params) => mock[ScriptDsl]
      val shutdownTask: Runnable = () => ()
      when(initialState.add(STARTED_STATE, handler)).thenReturn(updatedState)

      val scriptDsl = new FsmScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask, initialState)
      scriptDsl.add(STARTED_STATE, handler)

      verify(initialState).add(STARTED_STATE, handler)
      scriptDsl.getState should ===(updatedState)
    }
  }

  "shutdownScript" must {
    "execute the given shutdown task" in {
      val initialState = mock[FsmScriptState]

      var taskCalled = false
      val shutdownTask: Runnable = () => {
        taskCalled = true
      }
      val scriptDsl = new FsmScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask, initialState)

      scriptDsl.shutdownScript()
      taskCalled shouldBe true
    }
  }

  "execute" must {
    "delegate call to appropriate execute method defined on current script present in script state" in {
      val state                  = mock[FsmScriptState]
      val script                 = mock[ScriptDsl]
      val sequenceCommand        = mock[SequenceCommand]
      val futureUnit             = mock[Future[Unit]]
      val futureDone             = Future.successful(Done)
      val utcTime                = UTCTime.now()
      val hint                   = "datum"
      val ex                     = mock[Throwable]
      val shutdownTask: Runnable = () => ()

      when(state.currentScript).thenReturn(script)
      when(script.execute(sequenceCommand)).thenReturn(futureUnit)
      when(script.executeGoOnline()).thenReturn(futureDone)
      when(script.executeGoOnline()).thenReturn(futureDone)
      when(script.executeGoOffline()).thenReturn(futureDone)
      when(script.executeShutdown()).thenReturn(futureDone)
      when(script.executeAbort()).thenReturn(futureDone)
      when(script.executeStop()).thenReturn(futureDone)
      when(script.executeDiagnosticMode(utcTime, hint)).thenReturn(futureDone)
      when(script.executeOperationsMode()).thenReturn(futureDone)
      when(script.executeExceptionHandlers(ex)).thenReturn(futureDone)

      val scriptDsl = new FsmScriptDsl(seqOperatorFactory, logger, strandEc, shutdownTask, state)

      scriptDsl.execute(sequenceCommand) should ===(futureUnit)
      verify(script).execute(sequenceCommand)

      scriptDsl.executeGoOnline().futureValue should ===(Done)
      verify(script).executeGoOnline()

      scriptDsl.executeGoOffline().futureValue should ===(Done)
      verify(script).executeGoOffline()

      scriptDsl.executeShutdown().futureValue should ===(Done)
      verify(script).executeShutdown()

      scriptDsl.executeAbort().futureValue should ===(Done)
      verify(script).executeAbort()

      scriptDsl.executeStop().futureValue should ===(Done)
      verify(script).executeStop()

      scriptDsl.executeDiagnosticMode(utcTime, hint).futureValue should ===(Done)
      verify(script).executeDiagnosticMode(utcTime, hint)

      scriptDsl.executeOperationsMode().futureValue should ===(Done)
      verify(script).executeOperationsMode()

      scriptDsl.executeExceptionHandlers(ex)
      verify(script).executeExceptionHandlers(ex)
    }
  }
}
