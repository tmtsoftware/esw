package esw.ocs.dsl.script

import esw.ocs.dsl.params.Params

private[esw] case class FSMScriptState(
    params: Params,
    private val currentState: Option[String],
    private val maybeCurrentScript: Option[Params => ScriptDsl],
    private val stateHandlers: Map[String, Params => ScriptDsl]
) {
  lazy val currentScript: ScriptDsl =
    maybeCurrentScript.getOrElse(throw new RuntimeException("Current script handler is not initialized"))(params)

  def transition(nextState: String, params: Params): FSMScriptState =
    if (currentState.isEmpty || currentState.get != nextState) copy(params, Some(nextState), Some(getScript(nextState)))
    else this

  def add(state: String, script: Params => ScriptDsl): FSMScriptState =
    copy(stateHandlers = stateHandlers + (state -> script))

  private def getScript(state: String) =
    stateHandlers.getOrElse(state, throw new RuntimeException(s"No command handlers found for state: $state"))
}

object FSMScriptState {
  def init(): FSMScriptState = FSMScriptState(Params(), None, None, Map.empty)
}
