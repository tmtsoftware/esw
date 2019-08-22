package esw.ocs.testData

import csw.params.commands.CommandResponse.{Completed, Error}
import csw.params.commands.{CommandName, Sequence, Setup}
import csw.params.core.models.{Id, Prefix}
import csw.params.events.{EventName, SystemEvent}
import csw.time.core.models.{TAITime, UTCTime}
import esw.ocs.dsl.{CswServices, Script}

import scala.concurrent.duration.DurationDouble

class TestScript(csw: CswServices) extends Script(csw) {

  handleSetupCommand("command-1") { command =>
    spawn {
      // To avoid sequencer to finish immediately so that other Add, Append command gets time
      Thread.sleep(100)
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

  handleSetupCommand("command-4") { command =>
    spawn {
      //try sending concrete sequence
      val tcsSequencer = locationService.resolveSequencer("TCS", "testObservingMode4").await
      val command4     = Setup(Id("testCommandIdString123"), Prefix("TCS.test"), CommandName("command-to-assert-on"), None, Set.empty)
      val sequence     = Sequence(Id("testSequenceIdString123"), Seq(command4))

      csw.sequencerCommandService.submitSequence(tcsSequencer, sequence).await
      csw.crm.addOrUpdateCommand(Completed(command.runId))
    }
  }

  handleSetupCommand("fail-command") { command =>
    spawn {
      csw.crm.addOrUpdateCommand(Error(command.runId, command.commandName.name))
    }
  }

  handleSetupCommand("event-command") { command =>
    spawn {
      val event = SystemEvent(Prefix("TCS.test"), EventName("event-1"))

      // ***** commonly used dsl ****
      eventService.publish(event).await

      eventService.publish(5.seconds) {
        if (true) Some(event)
        else None
      }
      // *****************************

      eventService.publish(5.seconds, UTCTime.now()) {
        if (true) Some(event)
        else None
      }

      eventService.publish(5.seconds)({
        if (true) Some(event)
        else None
      }, {
        println
      })

      eventService.publish(5.seconds, TAITime.now())({
        if (true) Some(event)
        else None
      }, {
        println
      })
    }
  }
}
