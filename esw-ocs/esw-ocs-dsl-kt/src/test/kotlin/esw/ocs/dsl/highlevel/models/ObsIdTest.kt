package esw.ocs.dsl.highlevel.models

import csw.params.core.models.ObsId
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ObsIdTest {
    @Suppress("DANGEROUS_CHARACTERS")
    @Test
    fun `verify ObsId kotlin glue | ESW-421`() {
        val obsId = ObsId("2020A-001-123")
        val expectedObsId = ObsId.apply("2020A-001-123")

        obsId shouldBe expectedObsId
        obsId.programId shouldBe expectedObsId.programId()
        obsId.observationNumber shouldBe expectedObsId.observationNumber()
    }
}
