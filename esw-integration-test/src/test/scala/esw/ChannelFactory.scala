package esw

import java.nio.file.Files

object ChannelFactory {
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
