package esw.shell

import ammonite.util.Bind

import scala.sys.exit

//main to start the esw-shell
object Main extends App {
  def appVersion: String = BuildInfo.version
  def progName: String   = BuildInfo.name

  println(s"+++++ starting $progName-$appVersion +++++")

  val eswWiring: EswWiring = new EswWiring
  eswWiring.startLogging(progName, appVersion)

  val ammoniteResponse = ammonite
    .Main(
      predefCode = """ // #imports
                       |import java.nio.file.Path
                       |import java.nio.file.Paths
                       |import org.apache.pekko.util.Timeout
                       |import org.apache.pekko.Done
                       |import scala.concurrent.duration.{Duration, DurationDouble}
                       |import scala.concurrent.{Await, Future}
                       |import csw.alarm.models.AlarmSeverity
                       |import csw.alarm.models.Key.AlarmKey
                       |import csw.params.core.generics.KeyType.*
                       |import csw.params.core.generics.*
                       |import csw.params.events.*
                       |import csw.params.commands.*
                       |import csw.params.commands.CommandResponse.*
                       |import csw.params.core.models.*
                       |import csw.logging.models.Level.*
                       |import csw.prefix.models.Subsystem.*
                       |import csw.prefix.models.Prefix
                       |import csw.time.core.models.*
                       |import csw.params.core.states.*
                       |import csw.location.api.models.*
                       |import csw.location.api.models.ComponentType.*
                       |import csw.location.api.models.ConnectionType.*
                       |import csw.location.api.models.Connection.*
                       |import csw.logging.models.LogMetadata
                       |import csw.command.api.{DemandMatcher, DemandMatcherAll, PresenceMatcher}
                       |import esw.ocs.api.models.*
                       |import esw.ocs.api.protocol.*
                       |import esw.sm.api.models.ProvisionConfig
                       |import esw.sm.api.protocol.*
                       |import esw.agent.service.api.models.*
                       |import esw.shell.utils.*
                       |import esw.commons.extensions.FutureExt.*
                       |import esw.shell.utils.Timeouts.*
                       |val eswWiring = new esw.shell.EswWiring
                       |import eswWiring.*
                       |import eswWiring.factories.*
                       |import eswWiring.cswWiring.cswContext.*
                       |import csw.framework.scaladsl.DefaultComponentHandlers
                       |// #imports
                       |""".stripMargin
    )
    .run()
    ._1

  if (!ammoniteResponse.isSuccess) {
    println("Exiting the esw shell..")
    exit(1)
  }

}
