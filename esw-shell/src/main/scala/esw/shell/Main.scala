package esw.shell

object Main {
  def main(args: Array[String]): Unit = {
    println("+++++ starting esw.shell +++++")
    val eswWiring = new EswWiring
    ammonite
      .Main(
        predefCode = """
                |import java.nio.file.Path
                |import akka.util.Timeout
                |import akka.Done
                |import scala.concurrent.duration.{Duration, DurationDouble}
                |import scala.concurrent.{Await, Future}
                |import csw.params.core.generics.KeyType._
                |import csw.params.events._
                |import csw.params.commands._
                |import csw.params.commands.CommandResponse._
                |import csw.params.core.models._
                |import csw.logging.models.Level._
                |import csw.prefix.models.Subsystem._
                |import csw.prefix.models.Prefix
                |import csw.time.core.models._
                |import csw.params.core.states._
                |import csw.location.api.models.ComponentId
                |import csw.logging.models.LogMetadata
                |import csw.command.api.{DemandMatcher, DemandMatcherAll, PresenceMatcher}
                |import esw.ocs.api.models._
                |import esw.ocs.api.protocol._
                |import esw.sm.api.models.ProvisionConfig
                |import esw.sm.api.protocol._
                |import esw.agent.service.api.models._
                |import esw.shell.utils.Extensions._
                |import esw.shell.utils.Timeouts._
                |import commandService._
                |import eswWiring.cswWiring.cswContext._
                |""".stripMargin
      )
      .run(
        "eswWiring"              -> eswWiring,
        "commandService"         -> eswWiring.commandServiceDsl,
        "sequenceManagerService" -> eswWiring.sequenceManager _,
        "agentClient"            -> eswWiring.agentAkkaClient,
        "adminApi"               -> eswWiring.adminApi
      )
  }
}
