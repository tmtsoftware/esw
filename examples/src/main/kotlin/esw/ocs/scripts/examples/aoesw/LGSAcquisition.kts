package esw.ocs.scripts.examples.aoesw

import csw.params.commands.CommandResponse.Completed
import csw.params.commands.CommandResponse.Error
import csw.params.core.models.Choice
import csw.params.events.SystemEvent
import esw.ocs.dsl.core.script
import esw.ocs.dsl.params.*
import esw.ocs.dsl.utils.loop
import kotlin.math.sqrt
import kotlin.time.milliseconds

object aosq {
    val prefix = "aoesw.aosq"
}

object oiwfsDetectorAssembly {
    val name = "oiwfs-detector-assembly"
}

object rtcAssembly {
    val prefix = "nfiraos.rtc"
}

script {
    val oiwfsExposureModeChoices = choicesOf("SINGLE", "CONTINUOUS", "STOP", "NOOP")
    val oiwfsExposureModeKey = choiceKey("mode", oiwfsExposureModeChoices)

    val oiwfsStateEvent = eventKey(rtcAssembly.prefix, "oiwfsState")
    val oiwfsStateEnableChoices = choicesOf("NONE", "TT", "TTF")
    val oiwfsStateEnableKey = choiceKey("enable", oiwfsStateEnableChoices)
    val oiwfsStateFluxHighKey = booleanKey("fluxHigh")
    val oiwfsStateFluxLowKey = booleanKey("fluxlow")

    val ttfOffsetEvent = eventKey(rtcAssembly.prefix, "telOffloadTt") // ??
    val ttfOffsetXKey = floatKey("x")
    val ttfOffsetYKey = floatKey("y")

    val tcsOffsetCoordinateSystemChoices = choicesOf("RADEC", "XY", "ALTAZ")
    val tcsOffsetCoordSystemKey = choiceKey("coordinateSystem", tcsOffsetCoordinateSystemChoices)
    val tcsOffsetXKey = floatKey("x")
    val tcsOffsetYKey = floatKey("y")
    val tcsOffsetVirtualTelescopeChoices =
        choicesOf(
            "MOUNT",
            "OIWFS1",
            "OIWFS2",
            "OIWFS3",
            "OIWFS4",
            "ODGW1",
            "ODGW2",
            "ODGW3",
            "ODGW4",
            "GUIDER1",
            "GUIDER2"
        )
    val tcsOffsetVTKey = choiceKey("virtualTelescope", tcsOffsetVirtualTelescopeChoices)

    val loopeventKey = eventKey(rtcAssembly.prefix, "loop")
    val oiwfsLoopStatesChoices = choicesOf("IDLE", "LOST", "ACTIVE")
    val oiwfsLoopKey = choiceKey("oiwfsPoa", oiwfsLoopStatesChoices)

    fun handleOiwfsLoopOpen(oiwfsProbeNum: Int) {
        println(oiwfsProbeNum)
        // Do something
    }

    onEvent(loopeventKey.key()) { event ->
        when (event) {
            is SystemEvent -> {
                val oiwfsLoopStates = event(oiwfsLoopKey)
                val ii = oiwfsLoopStates.values.indexOf(Choice("LOST"))
                if (ii != -1) handleOiwfsLoopOpen(ii)
            }
        }
    }

    val TCSOFFSETTHRESHOLD = 2.0 // arcsec ???
    fun isOffsetRequired(x: Float, y: Float): Boolean = sqrt(x * x + y * y) > TCSOFFSETTHRESHOLD

    fun increaseExposureTime() {
        // not sure how this is done
    }

    suspend fun offsetTcs(xoffset: Float, yoffset: Float, probeNum: Int, obsId: String?) =
        submitSequence(
            "tcs", "darknight",
            sequenceOf(
                setup(aosq.prefix, "offset", obsId)
                    .add(tcsOffsetCoordSystemKey.set(Choice("RADEC")))
                    .add(tcsOffsetXKey.set(xoffset))
                    .add(tcsOffsetYKey.set(yoffset))
                    .add(tcsOffsetVTKey.set(Choice("OIWFS$probeNum")))
            )
        )

    handleSetup("enableOiwfsTtf") { command ->
        val ttfProbeNum = when (val event = getEvent(oiwfsStateEvent.key()).first()) {
            is SystemEvent -> event(oiwfsStateEnableKey).values.indexOf(Choice("TTF"))
            else -> -1
        }

        var ttfFluxHigh = false
        var ttfFluxLow = false
        var xoffset = 0.0f
        var yoffset = 0.0f

        val subscription = onEvent(oiwfsStateEvent.key(), ttfOffsetEvent.key()) { event ->
            when {
                event is SystemEvent && event.eventName() == oiwfsStateEvent.eventName() && ttfProbeNum != -1 -> {
                    ttfFluxHigh = event(oiwfsStateFluxHighKey)(ttfProbeNum)
                    ttfFluxLow = event(oiwfsStateFluxLowKey)(ttfProbeNum)
                }
                event is SystemEvent && event.eventName() == ttfOffsetEvent.eventName() -> {
                    xoffset = event(ttfOffsetXKey)(0)
                    yoffset = event(ttfOffsetYKey)(0)
                }
            }
        }

        // start continuous exposures on TTF probe
        val probeExpModes =
            (0..2).map { if (it == ttfProbeNum) Choice("CONTINUOUS") else Choice("NOOP") }.toTypedArray()
        val startExposureCommand = setup(aosq.prefix, "exposure", command.obsId)
            .add(oiwfsExposureModeKey.set(*probeExpModes))

        val response = submitAndWaitCommandToAssembly(oiwfsDetectorAssembly.name, startExposureCommand)

        when (response) {
            is Completed -> {
                val guideStarLockedThreshold = 5 // number of consecutive loops without an offset to consider stable
                var timesGuideStarLocked: Int = 0
                val maxAttempts = 20 // maximum number of loops on this guide star before rejecting
                var attempts = 0

                loop(500.milliseconds) {
                    when {
                        ttfFluxLow -> increaseExposureTime() // period tbd
                        isOffsetRequired(xoffset, yoffset) -> {
                            val offsetResponse = offsetTcs(xoffset, yoffset, ttfProbeNum, command.obsId)
                            println("offsetResponse = $offsetResponse")
                            timesGuideStarLocked = 0
                        }
                        ttfFluxHigh -> println() // do something
                        else -> timesGuideStarLocked += 1
                    }

                    attempts += 1
                    stopWhen((timesGuideStarLocked == guideStarLockedThreshold) || (attempts == maxAttempts))
                }

                if (timesGuideStarLocked == guideStarLockedThreshold) addOrUpdateCommand(Completed(command.runId()))
                else addOrUpdateCommand(Error(command.runId(), "Guide Star Unstable"))
            }
            else -> addOrUpdateCommand(Error(command.runId(), "Error starting WFS exposures: $response"))
        }
        subscription.cancel()
    }
}
