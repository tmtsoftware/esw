package esw.shell

import esw.testcommons.BaseTestSuite

class MainTest extends BaseTestSuite {

  private var process: Option[Process] = None

  "Main test should check imports" in {

    val channel        = "https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json"
    val version        = "0.1.0-SNAPSHOT"
    val commands       = List("cs", "launch", "--channel", channel, s"esw-shell:$version")
    val processBuilder = new ProcessBuilder(commands: _*)
    process = Some(processBuilder.start())
    Thread.sleep(10000)
    process.get.isAlive shouldBe true
  }

  override protected def afterAll(): Unit = {
    process.foreach(_.destroy())
    super.afterAll()
  }
}
