package esw.gateway.server

import esw.gateway.server.cli.{ArgsParser, Options}

object Main {
  def main(args: Array[String]): Unit = {
    new ArgsParser("esw-gateway-server").parse(args).map {
      case Options(port) =>
        val wiring = new Wiring(port)
        wiring.server.start
    }
  }
}
