package esw.ocs.script.server

import csw.prefix.models.Prefix

private[ocs] object OcsScriptServerApp {
  def main(args: Array[String]): Unit = {
    println(s"XXX OcsScriptServerApp args = ${args.toList}")
    if (args.length != 2) throw new RuntimeException("Expected two args: sequencerPrefix and sequenceComponentPrefix")
    val prefix                  = Prefix(args.head)
    val sequenceComponentPrefix = Prefix(args.tail.head)
    OcsScriptServerWiring(prefix, sequenceComponentPrefix)
  }
}
