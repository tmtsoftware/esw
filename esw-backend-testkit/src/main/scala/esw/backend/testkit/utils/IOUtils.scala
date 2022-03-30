/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.backend.testkit.utils

import java.nio.file.{Files, Path, StandardCopyOption}

import scala.util.Using

object IOUtils {

  def writeResourceToFile(resourceName: String, suffix: String = ".conf"): Path =
    Using.resource(getClass.getClassLoader.getResourceAsStream(resourceName)) { stream =>
      val tmpFile = Files.createTempFile(resourceName, suffix)
      Files.copy(stream, tmpFile, StandardCopyOption.REPLACE_EXISTING)
      tmpFile
    }

}
