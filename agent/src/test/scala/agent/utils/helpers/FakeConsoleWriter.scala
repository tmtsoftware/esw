package agent.utils.helpers

import agent.utils.ProcessOutput.ConsoleWriter

private[agent] class FakeConsoleWriter extends ConsoleWriter {
  var data: List[(String, Boolean)] = List.empty

  override def write(value: String): Unit    = data = data.appended((value, false))
  override def writeErr(value: String): Unit = data = data.appended((value, true))
}
