package esw.ocs.testData

import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.core.generics.KeyType.BooleanKey
import csw.params.core.models.Prefix
import csw.params.events.{EventName, SystemEvent}
import esw.ocs.impl.dsl.{CswServices, Script}

class TestScript2(csw: CswServices) extends Script(csw) {

  handleSetupCommand("command-1") { command =>
    spawn {
      // To avoid sequencer to finish immediately so that other Add, Append command gets time
      Thread.sleep(200)
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("command-2") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("command-3") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("fail-command") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Error(command.runId, command.commandName.name))
    }
  }

  handleGoOffline {
    spawn {
      // do some actions to go offline
      val param = BooleanKey.make("offline").set(true)
      val event = SystemEvent(Prefix("TCS.test"), EventName("offline")).add(param)
      csw.publishEvent(event).await
    }
  }

  handleGoOnline {
    spawn {
      // do some actions to go online
      val param = BooleanKey.make("online").set(true)
      val event = SystemEvent(Prefix("TCS.test"), EventName("online")).add(param)
      csw.publishEvent(event).await
    }
  }

  handleDiagnosticMode {
    case (startTime, hint) =>
      spawn {
        // do some actions to go to diagnostic mode based on hint
        csw.diagnosticModeForSequencer("testSequencerId6", "testObservingMode6", startTime, hint)
      }
  }

  handleOperationsMode {
    spawn {
      // do some actions to go to operations mode
      csw.operationsModeForSequencer("testSequencerId6", "testObservingMode6")
    }
  }
}
