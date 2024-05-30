package esw.shell

import java.util.concurrent.TimeUnit

import esw.testcommons.BaseTestSuite

class MainTest extends BaseTestSuite {

  // XXX Ignoring for now, since apps.json has not been updated to use this branch
  "verify esw-shell compiles and starts successfully" ignore {
//    val channel        = "https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json"
    val channel        = "https://raw.githubusercontent.com/tmtsoftware/osw-apps/Allan/pekko-scala3-update/apps.json"
    val version        = "0.1.0-SNAPSHOT"
    val commands       = List("cs", "launch", "--channel", channel, s"esw-shell:$version")
    val processBuilder = new ProcessBuilder(commands*)
    val process        = processBuilder.start()
    Thread.sleep(10000)
    assert(process.isAlive, "esw-shell failed to start!")

    process.descendants().map(_.destroyForcibly())
    process.destroyForcibly().waitFor(30, TimeUnit.SECONDS)

    assert(!process.isAlive, "esw-shell process did not terminate within 10 seconds!")
  }

}
