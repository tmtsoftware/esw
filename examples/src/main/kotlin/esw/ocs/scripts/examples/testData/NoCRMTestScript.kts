package esw.ocs.scripts.examples.testData

import esw.ocs.dsl.core.script
import kotlinx.coroutines.delay

script {

    /**
     * commands for testing success scenario for top level commands
     */
    handleSetup("command-1") { cmd ->
        println("command-1 handled")
        delay(800)
    }

    handleSetup("command-2") { cmd ->
        println("command-2 handled")
        delay(800)
    }
    /*************************************************************/

    /**
     * commands for testing 'exception' scenario for handle commands
     */
    handleSetup("command-3") { cmd ->
        println("command-3 handled")
        delay(800)
        throw RuntimeException("something went wrong")
    }

    handleSetup("command-4") { cmd ->
        println("command-4 handled")
        delay(800)
    }
    /*************************************************************/

    /**
     * commands for testing 'finishWithError' scenario for top level commands
     */
    handleSetup("command-5") { cmd ->
        println("command-5 handled")
        delay(800)
        finishWithError("something went wrong")
    }

    handleSetup("command-6") { cmd ->
        println("command-6 handled")
        delay(800)
    }
    /*************************************************************/
}