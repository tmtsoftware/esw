/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.commons.utils.config

import java.nio.file.Path

import csw.config.api.ConfigData
import csw.config.api.exceptions.FileNotFound
import csw.config.api.scaladsl.ConfigService
import csw.config.models.ConfigId
import esw.testcommons.BaseTestSuite
import org.mockito.Mockito

import scala.concurrent.Future
import org.mockito.Mockito.{verify, when}
class ConfigServiceExtTest extends BaseTestSuite {
  private val configService    = mock[ConfigService]
  private val configServiceExt = new ConfigServiceExt(configService)

  override protected def afterEach(): Unit = {
    Mockito.clearInvocations(configService)
    super.afterEach()
  }

  "ConfigServiceExt" must {
    val configId   = mock[ConfigId]
    val path       = Path.of(randomString(20))
    val configData = mock[ConfigData]

    "be able to update a config file at the given remote path with the given config data" in {
      val comment                 = "Update sequencer scripts version for test setup"
      val setActiveVersionComment = "setting updated version as active"

      when(configService.update(path, configData, comment)).thenReturn(Future.successful(configId))
      when(configService.setActiveVersion(path, configId, setActiveVersionComment)).thenReturn(Future.successful(()))

      configServiceExt.saveConfig(path, configData)
      verify(configService).update(path, configData, comment)
      verify(configService).setActiveVersion(path, configId, setActiveVersionComment)
    }

    "be able to create a config file at the given remote path with the given config data" in {
      val updateComment = "Update sequencer scripts version for test setup"
      val createComment = "Add sequencer scripts version for test setup"

      when(configService.update(path, configData, updateComment)).thenReturn(Future.failed(FileNotFound(path)))
      when(configService.create(path, configData, annex = false, createComment)).thenReturn(Future.successful(configId))

      configServiceExt.saveConfig(path, configData)
      verify(configService).update(path, configData, updateComment)
      verify(configService).create(path, configData, annex = false, createComment)
    }
  }
}
