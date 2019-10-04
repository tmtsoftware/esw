package esw.ocs.script

import akka.actor.Scheduler
import akka.actor.testkit.typed.javadsl.TestProbe
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
import csw.params.commands.*
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import csw.testkit.javadsl.FrameworkTestKitJunitResource
import esw.ocs.app.wiring.SequencerWiring
import io.kotlintest.*
import io.kotlintest.specs.FunSpec
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.TimeUnit
import scala.Option
import scala.collection.immutable.Set
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.jdk.javaapi.CollectionConverters
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

    private fun <T> List<T>.toScala() = CollectionConverters.asScala<T>(this).toSeq()
    private fun <T> kotlin.collections.Set<T>.toScala() = CollectionConverters.asScala<T>(this).toSet<T>()

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
        test("CswServices must be able to send sequence to other Sequencer by resolving location through TestScript | ESW-195, ESW-119") {
            val obsId = ObsId("obsId")
            val command = Setup(Prefix("TCS.test"), CommandName("command-4"), Optional.of(obsId))
            val submitResponseProbe = TestProbe.create<SubmitResponse>(actorSystem)
            val sequenceId = Id.apply()
            val sequence = Sequence(sequenceId, listOf(command).toScala())

            ocsSequencer.tell(SubmitSequenceAndWait(sequence, submitResponseProbe.ref))

            // This has to match with sequence created in TestScript -> handleSetupCommand("command-4")
            val assertableCommand =
                Setup(
                    Id("testCommandIdString123"),
                    Prefix("TCS.test"),
                    CommandName("command-to-assert-on"),
                    Option.empty(),
                    setOf<Parameter<*>>().toScala()
                )

            val assertableSequence = Sequence(Id("testSequenceIdString123"), listOf(assertableCommand).toScala())

            // response received by irisSequencer
            submitResponseProbe.expectMessage(CommandResponse.Completed(sequenceId))

            // sequence sent to tcsSequencer by irisSequencer script
            eventually(Duration.ofSeconds(10)) {
                sequenceReceivedByTCSProbe shouldBe assertableSequence
            }
        }
    }
}
