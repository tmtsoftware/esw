package esw.ocs.script

import akka.actor.Scheduler
import akka.actor.typed.*
import akka.actor.typed.javadsl.Behaviors
import akka.stream.typed.javadsl.ActorMaterializerFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SubmitSequenceAndWait
import csw.location.api.extensions.`ActorExtension$`
import csw.location.api.extensions.`URIExtension$`
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.models.AkkaRegistration
import csw.location.models.ComponentId
import csw.location.models.Connection
import csw.logging.client.commons.`AkkaTypedExtension$`
import csw.params.commands.CommandResponse
import csw.params.commands.Sequence
import csw.params.core.models.Prefix
import csw.testkit.javadsl.FrameworkTestKitJunitResource
import esw.ocs.app.wiring.SequencerWiring
import io.kotlintest.Spec
import io.kotlintest.TestCase
import io.kotlintest.TestResult
import io.kotlintest.specs.FunSpec
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeUnit
import scala.Option
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.Either

class ScriptIntegrationTest : FunSpec() {

    private val testKit = FrameworkTestKitJunitResource()
    private val actorSystem: ActorSystem<SpawnProtocol> = testKit.actorSystem()
    private val scheduler: Scheduler = actorSystem.scheduler()
    private val mat = ActorMaterializerFactory.create(actorSystem)

    private val ocsSequencerId = "testSequencerId4"
    private val ocsObservingMode = "testObservingMode4"
    private val askTimeout: Duration = Duration.ofSeconds(10)
    private val timeout = FiniteDuration(5, TimeUnit.SECONDS)
    private val tcsSequencer: ActorRef<SequencerMsg> = actorSystem.spawn(beh(), "testSequencer", Props.empty())
    private val tcsSequencerId = "TCS"
    private val tcsObservingMode = "testObservingMode4"

    private val tcsConnection =
        Connection.AkkaConnection(ComponentId("$tcsSequencerId@$tcsObservingMode", JComponentType.Sequencer))
    private val tcsRegistration =
        AkkaRegistration(tcsConnection, Prefix("TCS.test"), tcsSequencer.toUri())

    private lateinit var sequenceReceivedByTCSProbe: Sequence
    private lateinit var locationService: ILocationService
    private lateinit var ocsWiring: SequencerWiring
    private lateinit var ocsSequencer: ActorRef<SequencerMsg>

    private fun <T> ActorRef<T>.toUri() = `ActorExtension$`.`MODULE$`.RichActor(this).toURI()
    private fun URI.toActorRef(actorSystem: ActorSystem<*>) =
        `URIExtension$`.`MODULE$`.RichURI(this).toActorRef(actorSystem)

    private fun <L, R> Either<L, R>.rightValue(): R = this.toOption().get()
    private fun <L, R> Either<L, R>.leftValue(): L = this.left().get()
    private fun <T> Future<T>.await() = Await.result(this, timeout)
    private fun <T> ActorSystem<SpawnProtocol>.spawn(behavior: Behavior<T>, name: String, props: Props): ActorRef<T> =
        `AkkaTypedExtension$`.`MODULE$`.UserActorFactory(this).spawn(behavior, name, props)

    private fun beh(): Behavior<SequencerMsg> = Behaviors.receiveMessage<SequencerMsg> {
        when (it) {
            is SubmitSequenceAndWait -> {
                sequenceReceivedByTCSProbe = it.sequence()
                it.replyTo().tell(CommandResponse.Started(it.sequence().runId()))
                Behaviors.same<SequencerMsg>()
            }
            else -> Behaviors.same<SequencerMsg>()
        }
    }

    override fun beforeSpec(spec: Spec) = testKit.before()

    override fun afterSpec(spec: Spec) = testKit.after()

    override fun beforeTest(testCase: TestCase) {
        locationService = JHttpLocationServiceFactory.makeLocalClient(actorSystem, mat)
        locationService.register(tcsRegistration).get(5, TimeUnit.SECONDS)
        ocsWiring = SequencerWiring(ocsSequencerId, ocsObservingMode, Option.empty())
        ocsSequencer = ocsWiring.sequencerServer().start().rightValue().uri().toActorRef(actorSystem)
            .unsafeUpcast<SequencerMsg>()
    }

    override fun afterTest(testCase: TestCase, result: TestResult) {
        locationService.unregister(tcsConnection).get()
        ocsWiring.sequencerServer().shutDown().await()
    }

    init {
    }
}
