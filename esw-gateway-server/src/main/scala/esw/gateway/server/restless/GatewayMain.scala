package esw.gateway.server.restless

object GatewayMain {

  def main(args: Array[String]): Unit = {
    val wiring = new GatewayWiring(Some(8040))
    wiring.start()
  }

}
