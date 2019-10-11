package esw.ocs.testdata;

import csw.command.client.messages.TopLevelActorMessage;
import csw.framework.javadsl.JComponentBehaviorFactory;
import csw.framework.javadsl.JComponentHandlers;
import csw.framework.models.JCswContext;

public class AssemblyBehaviourFactory extends JComponentBehaviorFactory {
    @Override
    public JComponentHandlers jHandlers(akka.actor.typed.javadsl.ActorContext<TopLevelActorMessage> ctx, JCswContext cswCtx) {
        return new SampleComponentHandlers(ctx, cswCtx);
    }
}
