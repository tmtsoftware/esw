package esw

import java.nio.file.Files

import csw.logging.api.scaladsl.Logger
import csw.logging.client.scaladsl.GenericLoggerFactory
import esw.agent.app.process.cs.CoursierLaunch

object BinaryFetcherUtil {
  val logger: Logger = GenericLoggerFactory.getLogger

  def fetchBinaryFor(channel: String, coursierLaunch: CoursierLaunch, appVersion: Option[String] = None): Unit = {
    appVersion match {
      case Some(version) => logger.info(s"Fetching binary using coursier for $version")
      case None          => logger.info(s"Fetching binary using coursier for default version specified in provided $channel")
    }

    // provision app binary for specified version
    new ProcessBuilder(coursierLaunch.fetch(channel): _*)
      .inheritIO()
      .start()
      .waitFor()
  }

  def eswChannel(version: String): String = {
    val content =
      s"""
         |{
         |  "ocs-app": {
         |    "repositories": ["jitpack", "central"],
         |    "dependencies": [
         |      "com.github.tmtsoftware.esw:examples_2.13:$version"
         |    ],
         |    "mainClass": "esw.ocs.app.SequencerApp",
         |    "properties": {
         |      "csw-networks.hostname.automatic": "on",
         |      "csw-logging.appender-config.file.baseLogPath": "/tmp"
         |    }
         |  }
         |}
         |""".stripMargin

    "file://" + Files.write(Files.createTempFile("apps", "json"), content.getBytes)
  }

}
