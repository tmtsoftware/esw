package esw.ocs.script.server

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration, Metadata, NetworkType}
import csw.prefix.models.Prefix

object OcsScriptServerApp {
  def main(args: Array[String]): Unit = {
    println(s"XXX starting script server with args: ${args.mkString("Array(", ", ", ")")}")
    if (args.isEmpty) throw new RuntimeException("Missing required sequencer prefix argument")
    val prefix = Prefix(args.head)
    val wiring = OcsScriptServerWiring(prefix)
    import wiring.actorRuntime.{ec, typedSystem}

    wiring.server.start().foreach { binding =>
      val registration = HttpRegistration(
        connection = wiring.httpConnection,
        port = binding.localAddress.getPort,
        path = "",
        NetworkType.Inside,
        Metadata.empty
      )
      println(s"XXX 2 register $registration")
      wiring.locationService.register(registration)
    }
    println(s"XXX After script server reg")
  }
}
