package esw.ocs.scripts.examples.testData

import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.*
import kotlin.time.Duration
import kotlin.time.seconds

script {
    val lgsfSequencer = Sequencer(LGSF, ObsMode("darknight"), Duration.seconds(10))
    val testAssembly = Assembly(ESW, "test", Duration.seconds(10))

    onSetup("command-2") { command ->
        val dataWriteStart = sequencerObserveEvent.dataWriteStart(command.maybeObsId().get(), ExposureId("2021A-011-153-TCS-DET-SCI0-0001"))
        publishEvent(dataWriteStart)
    }

    onObserve("observe-start") {
        publishEvent(observeStart(ObsId("2021A-011-153")))
    }

    onObserve("observer-end") { command ->
        // ESW-421 demostrate usability of ObsId and ExposureId
        val exposureId = ExposureId("2021A-011-153-TCS-DET-SCI0-0001")

        // do something with ExposureId components
        println(exposureId.obsId.programId)
        println(exposureId.obsId.programId.semesterId)

        publishEvent(observeEnd(exposureId.obsId))
    }


    onObserve("exposure-start") {
        // ESW-421 demostrate usability of ObsId and ExposureId
        val obsId = ObsId("2021A-011-153")
        // do something with ObsId components
        println(obsId.programId)
        println(obsId.programId.semesterId)
        println(obsId.programId.semesterId.semester)

        // create exposureId
        val exposureId = ExposureId("${obsId}-TCS-DET-SCI0-0001")
        // do something with exposureId components
        println(exposureId.subsystem)
        println(exposureId.det)

        // ESW-81 demonstrate publishing of sequencer observe event exposureStart
        publishEvent(exposureStart(obsId, exposureId))
    }

    onGoOffline {
        // do some actions to go offline
        testAssembly.goOffline()
    }

    onGoOnline {
        // do some actions to go online
        testAssembly.goOnline()
    }

    onStop {
        //do some actions to stop
        val event = sequencerObserveEvent.exposureEnd(ObsId("2021A-011-153"), ExposureId("2021A-011-153-TCS-DET-SCI0-0001"))
        publishEvent(event)
        //send stop command to downstream sequencer
        lgsfSequencer.stop()
    }

}
