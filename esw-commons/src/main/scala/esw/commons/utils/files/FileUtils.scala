package esw.commons.utils.files

import java.nio.file.{Files, Path}
import scala.io.Source
import scala.util.Using

// FIXME: This should be moved to test scope or appropriate location
object FileUtils {

  private def stripExtension(fileName: String, ext: String = ".conf") = fileName.replaceAll(ext, "")

  def cpyFileToTmpFromResource(name: String): Path = {
    val tempConfigPath = Files.createTempFile(s"${stripExtension(name)}-", ".conf")
    Using(Source.fromResource(name)) { reader =>
      Files.write(tempConfigPath, reader.mkString.getBytes)
    }
    tempConfigPath.toFile.deleteOnExit()
    tempConfigPath
  }

  def readResource(resource: String): String = Source.fromResource(resource).mkString

}
