package esw.ocs.dsl.script

import esw.ocs.api.BaseTestSuite

class FSMScriptStateTest extends BaseTestSuite {
  private val script1 = () => mock[ScriptDsl]
  private val script2 = () => mock[ScriptDsl]

  private val INIT_STATE    = "INIT"
  private val STARTED_STATE = "STARTED"

  private val stateHandlers: Map[String, () => ScriptDsl] = Map(INIT_STATE -> script1, STARTED_STATE -> script2)

  "init" must {
    "initialize script state with empty values" in {
      FSMScriptState.init() should ===(FSMScriptState(None, None, Map.empty))
    }
  }

  "transition" must {
    "throw an exception when state is just initialized and handlers are not installed" in {
      val initialState = FSMScriptState.init()
      a[RuntimeException] shouldBe thrownBy(initialState.transition("INIT"))
    }

    "return same state when previous state == current state" in {
      val state = FSMScriptState(Some(INIT_STATE), None, Map.empty)
      state.transition(INIT_STATE) should ===(state)
    }

    "return new state when previous state =!= current state" in {
      val state = FSMScriptState(Some(INIT_STATE), Some(script1), stateHandlers)
      state.transition(STARTED_STATE) should ===(FSMScriptState(Some(STARTED_STATE), Some(script2), stateHandlers))
    }
  }

  "add" must {
    "allow adding new handlers against provided state in existing script state" in {
      val initialState = FSMScriptState.init()
      initialState.add(INIT_STATE, script1) should ===(FSMScriptState(None, None, Map(INIT_STATE -> script1)))
    }
  }

  "currentScript" must {
    "throw an exception when current script is not set in script state" in {
      val initialState = FSMScriptState.init()
      a[RuntimeException] shouldBe thrownBy(initialState.currentScript)
    }

    "return current script dsl when it is available in the script state" in {
      val state = FSMScriptState(Some(INIT_STATE), Some(script1), stateHandlers)
      state.currentScript shouldBe a[ScriptDsl]
    }
  }
}
