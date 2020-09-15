package shell

import esw.EswWiring

object Main {
  def main(args: Array[String]): Unit = {
    println("+++++ starting shell +++++")
    val eswWiring = new EswWiring
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
                |import esw.ocs.api.models.ObsMode
                |import commandService._
                |import eswWiring.shellWiring.cswContext._
                |import java.nio.file.Path
                |""".stripMargin
      )
      .run(
        "eswWiring"              -> eswWiring,
        "commandService"         -> eswWiring.commandServiceDsl,
        "sequenceManagerService" -> eswWiring.sequenceManager _,
        "agentClient"            -> eswWiring.agentAkkaClient
      )
  }
}
