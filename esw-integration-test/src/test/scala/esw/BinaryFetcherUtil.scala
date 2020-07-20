package esw

import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import esw.agent.app.process.cs.Coursier

object BinaryFetcherUtil {
  val logger: Logger = GenericLoggerFactory.getLogger

  def fetchBinaryFor(channel: String, appVersion: Option[String] = None): Unit = {
    appVersion match {
      case Some(version) => logger.info(s"Fetching binary using coursier for $version")
      case None          => logger.info(s"Fetching binary using coursier for default version specified in provided $channel")
    }

    // provision app binary for specified version
    new ProcessBuilder(Coursier.ocsApp(appVersion).fetch(channel): _*)
      .inheritIO()
      .start()
      .waitFor()
  }
}
