/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.script

import csw.params.core.generics.{KeyType, Parameter}
import esw.ocs.dsl.params.Params
import esw.testcommons.BaseTestSuite

class FsmScriptStateTest extends BaseTestSuite {
  private val script1 = (_: Params) => mock[ScriptDsl]
  private val script2 = (_: Params) => mock[ScriptDsl]

  private val INIT_STATE    = "INIT"
  private val STARTED_STATE = "STARTED"

  private val stateHandlers: Map[String, (Params) => ScriptDsl] = Map(INIT_STATE -> script1, STARTED_STATE -> script2)

  "init" must {
    "initialize script state with empty values" in {
      FsmScriptState.init() should ===(FsmScriptState(Params(), None, None, Map.empty))
    }
  }

  "transition" must {
    "throw an exception when state is just initialized and handlers are not installed" in {
      val initialState = FsmScriptState.init()
      a[RuntimeException] shouldBe thrownBy(initialState.transition("INIT", Params()))
    }

    "return same state when previous state == current state" in {
      val state = FsmScriptState(Params(), Some(INIT_STATE), None, Map.empty)
      state.transition(INIT_STATE, Params()) should ===(state)
    }

    "return same state when previous state == current state even if params are changed" in {
      val state                 = FsmScriptState(Params(), Some(INIT_STATE), None, Map.empty)
      val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(1)
      val newParams             = Params(java.util.Set.of(param))

      state.transition(INIT_STATE, newParams) should ===(state)
    }

    "return new state when previous state =!= current state" in {
      val state                 = FsmScriptState(Params(), Some(INIT_STATE), Some(script1), stateHandlers)
      val param: Parameter[Int] = KeyType.IntKey.make("encoder").set(1)
      val startedStateParams    = Params(java.util.Set.of(param))

      state.transition(STARTED_STATE, startedStateParams) should ===(
        FsmScriptState(startedStateParams, Some(STARTED_STATE), Some(script2), stateHandlers)
      )
    }
  }

  "add" must {
    "allow adding new handlers against provided state in existing script state" in {
      val initialState = FsmScriptState.init()
      initialState.add(INIT_STATE, script1) should ===(FsmScriptState(Params(), None, None, Map(INIT_STATE -> script1)))
    }
  }

  "currentScript" must {
    "throw an exception when current script is not set in script state" in {
      val initialState = FsmScriptState.init()
      a[RuntimeException] shouldBe thrownBy(initialState.currentScript)
    }

    "return current script dsl when it is available in the script state" in {
      val state = FsmScriptState(Params(), Some(INIT_STATE), Some(script1), stateHandlers)
      state.currentScript shouldBe a[ScriptDsl]
    }
  }
}
