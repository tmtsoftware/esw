package esw.ocs.scripts.examples.testData

import com.typesafe.config.ConfigFactory
import csw.alarm.models.Key.AlarmKey
import csw.params.events.Event
import csw.prefix.models.Prefix
import esw.ocs.dsl.core.script
import esw.ocs.dsl.highlevel.models.*
import esw.ocs.dsl.params.longKey
import kotlinx.coroutines.delay
import kotlin.time.seconds

script {
    onSetup("nonblocking-command") {
        delay(1200)
    }

    onSetup("blocking-command") {
        Thread.sleep(1200)
    }
}
