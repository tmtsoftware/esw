package esw

import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import esw.agent.app.AgentSettings
import esw.agent.app.process.cs.Coursier
import esw.ocs.testkit.EswTestKit

import scala.concurrent.duration.DurationLong

trait BinaryFetcherUtil extends EswTestKit {
  val logger: Logger = GenericLoggerFactory.getLogger

  def spawnAgentAndFetchBinaryFor(channelFileName: String, appVersion: Option[String] = None): Unit = {
    appVersion match {
      case Some(version) => logger.info(s"Fetching binary using coursier for $version")
      case None          => logger.info(s"Fetching binary using coursier for default version specified in provided $channelFileName")
    }

    val channel = "file://" + getClass.getResource(channelFileName).getPath
    spawnAgent(AgentSettings(1.minute, channel))

    // provision app binary for specified version
    new ProcessBuilder(Coursier.ocsApp(appVersion).fetch(channel): _*)
      .inheritIO()
      .start()
      .waitFor()
  }
}
