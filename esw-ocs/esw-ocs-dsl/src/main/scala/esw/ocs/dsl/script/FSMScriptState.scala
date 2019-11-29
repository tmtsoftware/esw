package esw.ocs.dsl.script

private[esw] case class FSMScriptState(
    private val currentState: Option[String],
    private val maybeCurrentScript: Option[() => ScriptDsl],
    private val stateHandlers: Map[String, () => ScriptDsl]
) {
  lazy val currentScript: ScriptDsl =
    maybeCurrentScript.getOrElse(throw new RuntimeException("Current script handler is not initialized"))()

  def transition(nextState: String): FSMScriptState =
    if (currentState.isEmpty || currentState.get != nextState) copy(Some(nextState), Some(getScript(nextState)))
    else this

  def add(state: String, script: () => ScriptDsl): FSMScriptState =
    copy(stateHandlers = stateHandlers + (state -> script))

  private def getScript(state: String) =
    stateHandlers.getOrElse(state, throw new RuntimeException(s"No command handlers found for state: $state"))
}

object FSMScriptState {
  def init(): FSMScriptState = FSMScriptState(None, None, Map.empty[String, () => ScriptDsl])
}
