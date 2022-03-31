/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.config

import java.nio.file.Path

import csw.config.api.ConfigData
import csw.config.api.exceptions.FileNotFound
import csw.config.api.scaladsl.ConfigService
import esw.commons.extensions.FutureExt.FutureOps

// FIXME: This should be moved to test scope or appropriate location
class ConfigServiceExt(configService: ConfigService) {

  def saveConfig(remotePath: Path, configData: ConfigData): Unit = {
    try {
      val id = configService
        .update(
          remotePath,
          configData,
          "Update sequencer scripts version for test setup"
        )
        .await()
      configService.setActiveVersion(remotePath, id, "setting updated version as active").await()
    }
    catch {
      case _: FileNotFound =>
        configService
          .create(
            remotePath,
            configData,
            annex = false,
            "Add sequencer scripts version for test setup"
          )
          .await()
    }
  }

}
