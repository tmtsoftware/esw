package esw.ocs.script.server

import csw.prefix.models.Prefix

object OcsScriptServerApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) throw new RuntimeException("Missing required sequencer prefix argument")
    val wiring = OcsScriptServerWiring(Prefix(args.head))
    wiring.server.start()
  }
}
