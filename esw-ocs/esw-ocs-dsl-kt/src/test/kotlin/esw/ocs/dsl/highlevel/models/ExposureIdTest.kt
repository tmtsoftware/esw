/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel.models

import csw.params.core.models.ExposureId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExposureIdTest {
    @Suppress("DANGEROUS_CHARACTERS")
    @Test
    fun `verify ExposureId kotlin glue | ESW-421`() {
        val exposureId = ExposureId("2021A-011-153-TCS-DET-SCI0-0001")
        val expectedExposureId = ExposureId.fromString("2021A-011-153-TCS-DET-SCI0-0001")

        exposureId shouldBe expectedExposureId
        exposureId.obsId shouldBe expectedExposureId.obsId().get()
        exposureId.det shouldBe expectedExposureId.det()
        exposureId.typLevel shouldBe expectedExposureId.typLevel()
        exposureId.subsystem shouldBe expectedExposureId.subsystem()
        exposureId.exposureNumber shouldBe expectedExposureId.exposureNumber()
    }
}
