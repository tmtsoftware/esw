package esw.ocs.scripts.examples.testData

import csw.params.core.models.ExposureId
import csw.params.core.models.ObsId
import esw.ocs.api.models.ObsMode
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.ESW
import esw.ocs.dsl.highlevel.models.LGSF
import kotlin.time.seconds

script {
    val lgsfSequencer = Sequencer(LGSF, ObsMode("darknight"), 10.seconds)
    val testAssembly = Assembly(ESW, "test", 10.seconds)

    onSetup("command-2") { command ->
        val dataWriteStart = sequencerObserveEvent.dataWriteStart(command.maybeObsId().get(), ExposureId.apply("2021A-011-153-TCS-DET-SCI0-0001"))
        publishEvent(dataWriteStart)
    }

    onObserve("observe-start") {
        publishEvent(sequencerObserveEvent.observeStart(ObsId.apply("2021A-011-153")))
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
        val event = sequencerObserveEvent.exposureEnd(ObsId.apply("2021A-011-153"), ExposureId.apply("2021A-011-153-TCS-DET-SCI0-0001"))
        publishEvent(event)
        //send stop command to downstream sequencer
        lgsfSequencer.stop()
    }

}
