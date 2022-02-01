@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import com.typesafe.config.Config
import csw.prefix.models.Prefix
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.WFOS
import esw.ocs.dsl.params.longKey
import esw.ocs.dsl.params.stringKey
import kotlin.time.Duration.Companion.minutes

//#motor-commands
/**
 * ======== Sample commands.conf file ========
 * wfos.motor.commands {
 *      set-motor-speed = 50
 *      set-step-motor-resolution = "1080p"
 * }
 */
data class MotorCommands(val setMotorSpeed: Long, val setStepMotorResolution: String) {

    // static factory to create `MotorCommands` from `Config` object
    // Ex. MotorCommands.from(config)
    companion object {
        fun from(wfosCommandsConfig: Config): MotorCommands {
            val motorCommandsConfig: Config = wfosCommandsConfig.getConfig("wfos.motor.commands")
            return MotorCommands(
                    motorCommandsConfig.getLong("set-motor-speed"),
                    motorCommandsConfig.getString("set-step-motor-resolution")
            )
        }
    }
}
//#motor-commands

script {
    val longTimeout = 10.minutes
    val motorPrefix = Prefix(WFOS, "motor")
    val motorPrefixStr = motorPrefix.toString()
    val motorHcd = Hcd(motorPrefix, longTimeout)
    val motorSpeedKey = longKey("motor-speed")
    val motorResolutionKey = stringKey("motor-resolution")

    //#exists-config
    val commandsFile = "/wfos/commands.conf"
    val commandsConfigExist: Boolean = existsConfig(commandsFile)

    // terminate script if required configuration file does not exist
    if (!commandsConfigExist) finishWithError("Configuration file [$commandsFile] not found in configuration service")
    //#exists-config

    //#get-config
    val wfosCommandsFile = "/wfos/commands.conf"

    // retrieve configuration file from config service, terminate script if configuration file does not exist
    val commandsConfig: Config = getConfig(wfosCommandsFile)
            ?: finishWithError("Configuration file [$wfosCommandsFile] not found in configuration service")

    val motorCommands = MotorCommands.from(commandsConfig)

    // on receiving `set-motor-speed` command, send `set-speed` command to downstream motor hcd
    onSetup("set-motor-speed") {
        val motorSpeedParam = motorSpeedKey.set(motorCommands.setMotorSpeed)
        val setSpeedCommand = Setup(motorPrefixStr, "set-speed").add(motorSpeedParam)
        motorHcd.submit(setSpeedCommand)
    }

    // on receiving `set-step-motor-resolution` command, send `set-resolution` command to downstream motor hcd
    onSetup("set-step-motor-resolution") {
        val setResolutionParam = motorResolutionKey.set(motorCommands.setStepMotorResolution)
        val setResolutionCommand = Setup(motorPrefixStr, "set-resolution").add(setResolutionParam)
        motorHcd.submit(setResolutionCommand)
    }
    //#get-config
}
