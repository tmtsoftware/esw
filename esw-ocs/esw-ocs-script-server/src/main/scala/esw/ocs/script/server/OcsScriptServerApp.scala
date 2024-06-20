package esw.ocs.script.server

import csw.location.api.models.Connection.HttpConnection
import csw.location.api.models.{ComponentId, ComponentType, HttpRegistration, Metadata, NetworkType}
import csw.prefix.models.Prefix

object OcsScriptServerApp {
  def main(args: Array[String]): Unit = {
    if (args.isEmpty) throw new RuntimeException("Missing required sequencer prefix argument")
    val prefix = Prefix(args.head)
    val wiring = OcsScriptServerWiring(prefix)
    import wiring.actorRuntime.{ec, typedSystem}

    val httpConnection: HttpConnection = HttpConnection(ComponentId(prefix, ComponentType.Service))

    wiring.server.start().foreach { binding =>
      val registration = HttpRegistration(
        connection = httpConnection,
        port = binding.localAddress.getPort,
        path = "",
        NetworkType.Inside,
        Metadata.empty
      )
      wiring.locationService.register(registration)
    }
  }
}
