package esw.shell

import esw.testcommons.BaseTestSuite

class MainTest extends BaseTestSuite {

  "verify esw-shell compiles and starts successfully" in {
    val channel        = "https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json"
    val version        = "0.1.0-SNAPSHOT"
    val commands       = List("cs", "launch", "--channel", channel, s"esw-shell:$version")
    val processBuilder = new ProcessBuilder(commands: _*)
    val process        = processBuilder.inheritIO().start()
    Thread.sleep(10000)
    assert(process.isAlive, "esw-shell failed to start!")

    process.descendants().map(_.destroyForcibly())
    process.destroyForcibly()
  }

  // just to verify if this test halts github runner
  override protected def afterAll(): Unit = println("========== ESW-SHELL TERMINATED ==========")

}
