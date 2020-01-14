@file:Suppress("UNUSED_VARIABLE")

package esw.ocs.scripts.examples.paradox

import com.typesafe.config.Config
import esw.ocs.dsl.core.script

script {

    onObserve("check-config") {
        //#exists-config
        val bootConfExist: Boolean = existsConfig("/wfos/boot.conf")
        //#exists-config

        if (bootConfExist) {
            //#get-config
            val bootConf: Config? = getConfig("/wfos/boot.conf")
            //#get-config
        }
    }
}