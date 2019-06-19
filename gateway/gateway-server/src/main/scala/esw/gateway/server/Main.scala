package esw.gateway.server

import esw.gateway.server.cli.{ArgsParser, Options}

import scala.concurrent.Await
import scala.concurrent.duration.DurationInt

object Main {
  def main(args: Array[String]): Unit = {
    new ArgsParser("http-server").parse(args).map {
      case Options(port) =>
        val wiring = new Wiring(port)
        Await.result(wiring.httpService.registeredLazyBinding, 15.seconds)
    }
  }
}
