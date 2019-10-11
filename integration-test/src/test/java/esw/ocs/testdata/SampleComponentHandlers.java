package esw.ocs.testdata;

import akka.actor.typed.javadsl.ActorContext;
import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;
import csw.location.models.TrackingEvent;
import csw.logging.api.javadsl.ILogger;
import csw.params.commands.CommandResponse;
import csw.params.commands.CommandResponse.Accepted;
import csw.params.commands.CommandResponse.Completed;
import csw.params.commands.ControlCommand;
import csw.params.core.generics.Parameter;
import csw.params.core.models.Prefix;
import csw.params.events.EventName;
import csw.params.events.SystemEvent;
import csw.params.javadsl.JKeyType;
import csw.time.core.models.UTCTime;

import java.util.concurrent.CompletableFuture;

public class SampleComponentHandlers extends JComponentHandlers {
    ActorContext<TopLevelActorMessage> ctx;
    JCswContext cswCtx;
    ILogger log;
    public SampleComponentHandlers(ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        super(ctx, cswCtx);
        this.cswCtx = cswCtx;
        this.ctx = ctx;
        this.log = cswCtx.loggerFactory().getLogger(ctx, getClass());
    }

    @Override
    public CompletableFuture<Void> jInitialize() {
        log.info("Initializing Component TLA");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> jOnShutdown() {
        log.info("Shutting down Component TLA");
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void onLocationTrackingEvent(TrackingEvent trackingEvent) {

    }

    @Override
    public CommandResponse.ValidateCommandResponse validateCommand(ControlCommand controlCommand) {
        return new Accepted(controlCommand.runId());
    }

    @Override
    public CommandResponse.SubmitResponse onSubmit(ControlCommand controlCommand) {
        return new Completed(controlCommand.runId());
    }

    @Override
    public void onOneway(ControlCommand controlCommand) {

    }

    @Override
    public void onDiagnosticMode(UTCTime startTime, String hint) {
        Parameter<String> diagnosticModeParam = JKeyType.StringKey().make("mode").set("diagnostic");
        SystemEvent event = new SystemEvent(Prefix.apply("tcs.filter.wheel"), EventName.apply("diagnostic-data")).add(diagnosticModeParam);
        cswCtx.eventService().defaultPublisher().publish(event);
    }

    @Override
    public void onOperationsMode() {
        Parameter<String> diagnosticModeParam = JKeyType.StringKey().make("mode").set("operations");
        SystemEvent event = new SystemEvent(Prefix.apply("tcs.filter.wheel"), EventName.apply("diagnostic-data")).add(diagnosticModeParam);
        cswCtx.eventService().defaultPublisher().publish(event);
    }

    @Override
    public void onGoOffline() {
        SystemEvent event = new SystemEvent(Prefix.apply("tcs.filter.wheel"), EventName.apply("offline"));
        cswCtx.eventService().defaultPublisher().publish(event);
    }

    @Override
    public void onGoOnline() {
        SystemEvent event = new SystemEvent(Prefix.apply("tcs.filter.wheel"), EventName.apply("online"));
        cswCtx.eventService().defaultPublisher().publish(event);
    }
}
