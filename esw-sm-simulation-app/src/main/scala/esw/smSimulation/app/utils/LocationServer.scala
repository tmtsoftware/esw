package esw.smSimulation.app.utils

import csw.location.server.{Main => LocationMain}

object LocationServer {
  private val serviceName = "Location Service"

  def start(clusterPort: String) =
    LocationMain.start(Array("--clusterPort", clusterPort))

//  private val stop: Option[(Http.ServerBinding, ServerWiring)] => Unit = _.foreach {
//    case (_, wiring) => wiring.actorRuntime.shutdown().await()
//  }

}
