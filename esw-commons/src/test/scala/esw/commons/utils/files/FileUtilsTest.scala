package esw.commons.utils.files

import esw.testcommons.BaseTestSuite

import scala.io.Source

class FileUtilsTest extends BaseTestSuite {
  "cpyFileToTmpFromResource" must {
    "copy contents of resource file to a temp file" in {
      val path   = FileUtils.cpyFileToTmpFromResource("testFile.txt")
      val source = Source.fromFile(path.toUri)
      source.getLines().mkString shouldBe "Some random string text"
      source.close()
    }
  }
}
