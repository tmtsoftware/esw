package esw.services.utils

import java.nio.file.{Path, Paths}

object PathUtils {
  def getResourcePath(name: String): Path = {
    Paths.get(getClass.getResource(s"/$name").getPath)
  }
}
