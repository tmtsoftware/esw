package shell

import csw.framework.ShellWiring
import esw.CommandServiceDsl

object Main {

  def main(args: Array[String]): Unit = {
    println("+++++ starting shell +++++")
    val shellWiring = new ShellWiring
    ammonite
      .Main(
        predefCode = """
                |import akka.util.Timeout
                |import scala.concurrent.duration.{Duration, DurationDouble}
                |import scala.concurrent.{Await, Future}
                |import csw.params.core.generics.KeyType._
                |import csw.params.events._
                |import csw.params.commands._
                |import csw.params.core.models._
                |import csw.prefix.models.Subsystem._
                |import csw.prefix.models.Prefix
                |import shell.utils.Extensions._
                |import shell.utils.Timeouts._
                |import commandService._
                |import commandService.shellWiring.cswContext._
                |""".stripMargin
      )
      .run(
        "commandService" -> new CommandServiceDsl(shellWiring)
      )
  }
}
