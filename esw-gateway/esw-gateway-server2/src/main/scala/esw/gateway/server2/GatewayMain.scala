package esw.gateway.server2

object GatewayMain {

  def main(args: Array[String]): Unit = {
    val wiring = new GatewayWiring()
    import wiring._
    httpService.registeredLazyBinding
  }

}
