/*
 * Copyright (c) 2022 Thirty Meter Telescope International Observatory
 * SPDX-License-Identifier: Apache-2.0
 */

package esw.ocs.dsl.highlevel

import csw.params.commands.CommandName
import csw.params.commands.Observe
import csw.params.commands.Setup
import csw.params.core.models.ObsId
import csw.prefix.models.Prefix
import esw.ocs.dsl.highlevel.models.ESW
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.*

@Suppress("DANGEROUS_CHARACTERS")
class CommandServiceDslTest : CommandServiceDsl {

    @Test
    fun `setup method should construct a Setup command with given prefix, commandName and obsId | ESW-121`() = runBlocking {
        val actualSetupCommand = Setup("esw.test", "move", "2020A-001-123")
        val expectedSetupCommand = Setup(Prefix(ESW, "test"), CommandName("move"), Optional.of(ObsId.apply("2020A-001-123")))

        actualSetupCommand.source() shouldBe expectedSetupCommand.source()
        actualSetupCommand.commandName() shouldBe expectedSetupCommand.commandName()
        actualSetupCommand.maybeObsId() shouldBe expectedSetupCommand.maybeObsId()
    }

    @Test
    fun `observe method should construct a Observe command with given prefix, commandName and obsId | ESW-121`() = runBlocking {
        val expectedObserveCommand = Observe(Prefix(ESW, "test"), CommandName("move"), Optional.of(ObsId.apply("2020A-001-123")))
        val actualObserveCommand = Observe("esw.test", "move", "2020A-001-123")
        actualObserveCommand.source() shouldBe expectedObserveCommand.source()
        actualObserveCommand.commandName() shouldBe expectedObserveCommand.commandName()
        actualObserveCommand.maybeObsId() shouldBe expectedObserveCommand.maybeObsId()
    }

}
