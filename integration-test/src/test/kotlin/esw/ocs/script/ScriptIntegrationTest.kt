package esw.ocs.script

import akka.actor.testkit.typed.javadsl.TestProbe
import akka.actor.typed.*
import akka.actor.typed.javadsl.AskPattern
import akka.actor.typed.javadsl.Behaviors
import akka.japi.function.Function
import akka.stream.typed.javadsl.ActorMaterializerFactory
import com.typesafe.config.ConfigFactory
import csw.command.client.messages.sequencer.SequencerMsg
import csw.command.client.messages.sequencer.SubmitSequenceAndWait
import csw.event.client.EventServiceFactory
import csw.location.api.extensions.`ActorExtension$`
import csw.location.api.extensions.`URIExtension$`
import csw.location.api.javadsl.ILocationService
import csw.location.api.javadsl.JComponentType
import csw.location.client.javadsl.JHttpLocationServiceFactory
import csw.location.models.AkkaRegistration
import csw.location.models.ComponentId
import csw.location.models.Connection
import csw.logging.client.commons.`AkkaTypedExtension$`
import csw.params.commands.CommandName
import csw.params.commands.CommandResponse
import csw.params.commands.CommandResponse.SubmitResponse
import csw.params.commands.Sequence
import csw.params.commands.Setup
import csw.params.core.generics.Parameter
import csw.params.core.models.Id
import csw.params.core.models.ObsId
import csw.params.core.models.Prefix
import csw.params.events.Event
import csw.params.events.EventKey
import csw.params.events.EventName
import csw.params.events.SystemEvent
import csw.testkit.javadsl.FrameworkTestKitJunitResource
import csw.testkit.javadsl.JCSWService
import csw.time.core.models.UTCTime
import esw.ocs.api.protocol.DiagnosticModeResponse
import esw.ocs.api.protocol.OperationsModeResponse
import esw.ocs.api.protocol.`Ok$`
import esw.ocs.app.wiring.SequencerWiring
import esw.ocs.dsl.params.stringKey
import esw.ocs.impl.messages.SequencerMessages.DiagnosticMode
import esw.ocs.impl.messages.SequencerMessages.OperationsMode
import io.kotlintest.*
import io.kotlintest.specs.FunSpec
import kotlinx.coroutines.future.await
import scala.Option
import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.jdk.javaapi.CollectionConverters
import scala.util.Either
import java.net.URI
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit

class ScriptIntegrationTest : FunSpec() {

    private val testKit = FrameworkTestKitJunitResource(listOf(JCSWService.EventServer))
    private val actorSystem: ActorSystem<SpawnProtocol> = testKit.actorSystem()
    private val mat = ActorMaterializerFactory.create(actorSystem)

    private val ocsSequencerId = "testSequencerId4"
    private val ocsObservingMode = "testObservingMode4"
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
    private fun <T> Future<T>.await() = Await.result(this, timeout)
    private fun <T> ActorSystem<SpawnProtocol>.spawn(behavior: Behavior<T>, name: String, props: Props): ActorRef<T> =
        `AkkaTypedExtension$`.`MODULE$`.UserActorFactory(this).spawn(behavior, name, props)

    private infix fun <T, R> ActorRef<T>.ask(msg: (ActorRef<R>) -> T): CompletionStage<R> = AskPattern.ask<T, R>(this,
        Function { replyTo -> msg(replyTo) }
        , 10.seconds, actorSystem.scheduler())

    private fun <T> List<T>.toScala() = CollectionConverters.asScala<T>(this).toSeq()
    private fun <T> Set<T>.toScala() = CollectionConverters.asScala<T>(this).toSet<T>()

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
            val assertableCommand = Setup(
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

        test("CswServices must be able to forward diagnostic mode to downstream components | ESW-118") {
            val eventService =
                EventServiceFactory().jMake(JHttpLocationServiceFactory.makeLocalClient(actorSystem, mat), actorSystem)
            val eventKey = EventKey(Prefix("tcs.filter.wheel"), EventName("diagnostic-data"))

            val load = ConfigFactory.parseString (
                """
                    name = "test"
                    componentType = assembly
                    behaviorFactoryClassName = esw.ocs.testdata.AssemblyBehaviourFactory
                    prefix = esw.test
                    locationServiceUsage = RegisterOnly
                    connections = []
                """.trimIndent()
            )
            testKit.spawnStandalone(load)
            val testProbe = TestProbe.create<Event>(actorSystem)

            val subscription = eventService.defaultSubscriber().subscribeActorRef(setOf(eventKey), testProbe.ref)
            subscription.ready().await()
            testProbe.expectMessageClass(SystemEvent::class.java)

            //diagnosticMode
            val diagnosticModeParam = stringKey("mode").set(listOf("diagnostic").toScala())
            val diagnosticModeResF: CompletionStage<DiagnosticModeResponse> =
                ocsSequencer ask { replyTo: ActorRef<DiagnosticModeResponse> ->
                    DiagnosticMode(UTCTime.now(), "engineering", replyTo)
                }

            diagnosticModeResF.await() shouldBe `Ok$`.`MODULE$`

            val expectedDiagEvent = testProbe.expectMessageClass(SystemEvent::class.java)
            expectedDiagEvent.paramSet().head() shouldBe diagnosticModeParam

            //operationsMode
            val operationsModeParam = stringKey("mode").set(listOf("operations").toScala())

            val operationsModeResF: CompletionStage<OperationsModeResponse> =
                ocsSequencer ask { replyTo: ActorRef<OperationsModeResponse> ->
                    OperationsMode(replyTo)
                }
            operationsModeResF.await() shouldBe `Ok$`.`MODULE$`

            val expectedOpEvent = testProbe.expectMessageClass(SystemEvent::class.java)
            expectedOpEvent.paramSet().head() shouldBe operationsModeParam
        }
    }
}
