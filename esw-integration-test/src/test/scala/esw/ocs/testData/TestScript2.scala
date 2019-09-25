//package esw.ocs.testData
//
//import java.util.concurrent.CompletableFuture
//
//import csw.params.commands.CommandResponse.{Completed, Error}
//import csw.params.core.generics.KeyType.BooleanKey
//import csw.params.core.models.Prefix
//import csw.params.events.{EventName, SystemEvent}
//import esw.dsl.script.CswServices
//import esw.dsl.script.javadsl.JScript
//
//import scala.compat.java8.FutureConverters.FutureOps
//
//class TestScript2(csw: CswServices) extends JScript(csw) {
//
//  jHandleSetupCommand("command-1") { command =>
//    // To avoid sequencer to finish immediately so that other Add, Append command gets time
//    Thread.sleep(200)
//    csw.crm.addOrUpdateCommand(Completed(command.runId))
//    CompletableFuture.completedFuture(null)
//  }
//
//  jHandleSetupCommand("command-2") { command =>
//    csw.crm.addOrUpdateCommand(Completed(command.runId))
//    CompletableFuture.completedFuture(null)
//  }
//
//  jHandleSetupCommand("command-3") { command =>
//    csw.crm.addOrUpdateCommand(Completed(command.runId))
//    CompletableFuture.completedFuture(null)
//  }
//
//  jHandleSetupCommand("fail-command") { command =>
//    csw.crm.addOrUpdateCommand(Error(command.runId, command.commandName.name))
//    CompletableFuture.completedFuture(null)
//  }
//
//  jHandleGoOffline {
//    // do some actions to go offline
//    val param = BooleanKey.make("offline").set(true)
//    val event = SystemEvent(Prefix("TCS.test"), EventName("offline")).add(param)
//    () => csw._eventService.defaultPublisher.publish(event).toJava.
//  }
//
//  jHandleGoOnline {
//    // do some actions to go online
//    val param = BooleanKey.make("online").set(true)
//    val event = SystemEvent(Prefix("TCS.test"), EventName("online")).add(param)
//    () => csw._eventService.defaultPublisher.publish(event).map(_ => null).toJava
//  }
//
//  jHandleDiagnosticMode {
//    case (startTime, hint) =>
//      // do some actions to go to diagnostic mode based on hint
//      csw.sequencerAdminFactory
//        .make("testSequencerId6", "testObservingMode6")
//        .flatMap(_.diagnosticMode(startTime, hint))
//        .map(_ => Void)
//        .toJava
//  }
//
//  jHandleOperationsMode {
//    // do some actions to go to operations mode
//    () =>
//      csw.sequencerAdminFactory
//        .make("testSequencerId6", "testObservingMode6")
//        .flatMap(_.operationsMode())
//        .map(_ => Void)
//        .toJava
//  }
//}
