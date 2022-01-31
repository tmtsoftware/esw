# OSW Testing Philosophy

This document serves to describe the OSW Testing Philosophy, using the Sequence Manager code as an
example. The philosophy can be summarized with the belief that unit tests should be favored over
"integrated" component tests, in the sense that a comprehensive set of unit tests paired with a small set of
component tests can provide a complete test suite without the need for a large set of component
tests that pursue every possible code path.  

Software today often follows the
[Single Responsibility Principle](https://en.wikipedia.org/wiki/Single-responsibility_principle),
in which code is often broken down into components and layers that cover one function of the program.
This leads to more isolated “units” to test, but branching at each layer of code can lead to an
exponential number of code paths to cover. Writing a complete set of integrated tests that explore
every branch of code can be extremely time consuming or even impossible.

Testing with a full set of component tests can actually be entirely unnecessary. A comprehensive
set of unit tests, when written properly, can demonstrate every code path sufficiently. Then, they
can be combined with a few component tests as both a sanity check and to ensure the connections
are complete throughout the layers of code.

## Layered Software

As mentioned, it is common that software today is composed of layers, each with its own
responsibility. This facilitates understanding of the software as well as making it easier to test.
When we talk about the ordering of layers, we often start with the outward facing API layer as the
top-most. This layer then will call methods of the layer below. That layer may call methods of the
layer below it, an so on. It is important to note that typically a lower layer does not call methods
of the layer above it. It simply returns responses back up the layers to the external user. A layer
may have call methods from more than one class and therefore can have multiple layers directly
beneath it. The layers directly beneath are called dependencies.

An important facet of our component construction is that, it follows the
[Dependency Injection](https://en.wikipedia.org/wiki/Dependency_injection) pattern. For each layer
in our component, its dependencies are injected into the layer by passing them in the constructor of
the layer. For OSW software, this is done by instantiating all layers in a wiring class. Lowest
layer dependencies are instantiated first, and then layers are created going up the layers, each
time passing in the dependencies for the layers below. The constructor of each layer should use the
interface of the dependency, so that various implementations of the dependency can be passed in,
such as a version for testing or simulation. This also allows mocks to be used for each dependency.
This is essential for layered testing.

## Layered Testing

Layered testing forms the basis of our testing strategy. The fundamental idea is that if we can
assume the dependency of a layer is tested and correct, we can then test the layer independent of
the dependency by mocking it. In other words, if our software package is constructed using layers as
described above, the entire package can be thoroughly tested by the implementing the layer class by
using mocks or stubs for its dependencies.

In some cases, a layer is nothing more than translation or adapter layer which maps methods to some
other protocol call to the layer below. An example of this is an Akka client layer that maps methods
to Akka ask or tell calls to some Actor below it. In these case, all that is needed is to
demonstrate is that each method maps to the appropriate request or method of the layer below. This
is done by mocking the method call of the dependency and verifying the appropriate method is called
when the client method is called.

## Client-Server interfaces

Any remoting or Client-Server type interface that requires serialization of messages or commands
must demonstrate proper serialization and deserialization on both sides of the interface for all
models that can sent and received over the wire.  

## Sequence Manager as an Example

To start, we will go through the Sequence Manager code and tests to show the multiple layers
involved and how testing is done at each one.  

We’ll start with a look at the [SequenceManagerApi]($github.base_url$/esw-sm/esw-sm-api/shared/src/main/scala/esw/sm/api/SequenceManagerApi.scala):

```scala
trait SequenceManagerApi {

  /**
   * Configures sequencers and resources needed for provided observing mode.
   */
  def configure(obsMode: ObsMode): Future[ConfigureResponse]

  /**
   * Shutdown running sequencer of provided Subsystem, Observing mode and optional parameter Variation
   */
  def shutdownSequencer(
    subsystem: Subsystem,
    obsMode: ObsMode,
    variation: Option[Variation] = None
  ): Future[ShutdownSequencersResponse]

  // other APIs ...
}
```

The Sequence Manager provides both an HTTP and Akka interface to it, so it has two implementations
of the API: a HTTP client and an Akka Client. The core functionality of the program is written as an
Akka actor, and the HTTP service is merely an adaptive wrapper around the Akka interface, in that
the HTTP service contains the Akka Client, and translates HTTP requests into Akka calls to the
Sequence Manager Actor Behavior.

The HTTP server in the Sequence Manager is implemented using
[msocket](https://github.com/tmtsoftware/msocket) without any streams or callback methods
(i.e. no websocket are needed), so only a “post-handler” class needs to be implemented. This class
merely translates msocket requests, that is, POST requests to the “post-endpoint”, to SM API calls
in the Akka implementation.

This program has many layers to it, summarized by the following list:

- **SequenceManagerClient**: HTTP client, which provides a facade for post requests, which are
  received by an msocket-based HTTP service. These requests are processed in the

- **SequenceManagerRequestHandler**: This takes the HTTP requests and, based on the request model,
  translates them to commands in the

- **SequenceManagerImpl**: an Akka client to the SequenceManager actor, in which method calls are
  translated to Akka messages, sent to the actor, and handled in the

- **SequenceManagerBehavior**: the actor behavior which receives the Akka messages and delegates to
  utility classes, which sometimes is

- **SequencerUtil**: which performs operations on Sequencers.  It uses the Location Service, and
  sometimes needs to use

- **SequenceComponentUtil**: which performs operations on Sequence Components, such as killing them.
  This requires interaction with the Location Service.

Each layer must have a unit test that mocks the behavior of the layer below. Each test must verify
every available method or interface of the layer below and all unique return types generated in the
layer must be demonstrated.

Each of these classes are actually created in the **SequenceManagerWiring** class. This allows the
class for the layer below to be passed in as an argument to the layer above. This is essential to
dependency injection because it makes mocking that lower layer possible.

We will take a look at each layer, and examine the way it is defined and how to write the tests. We
will focus on one particular method, `shutdownSequencer(subsystem: Subsystem, obsMode: ObsMode,variation: Option[Variation] = None)`,
because it is complete enough to demonstrate our testing philosophy, but simple enough to not
over complicate this document. We will take a top-down look at the layers, so we will start with the
HTTP Client.  

### SequenceManagerClient

The [SequenceManagerClient]($github.base_url$/esw-sm/esw-sm-api/shared/src/main/scala/esw/sm/api/client/SequenceManagerClient.scala)
class has the msocket POST client passed in. The following snippet shows the implementation of
`shutdownSequencer`:

```scala
override def shutdownSequencer(
  subsystem: Subsystem,
  obsMode: ObsMode,
  variation: Option[Variation] = None
): Future[ShutdownSequencersResponse] =
  postClient.requestResponse[ShutdownSequencersResponse](
    ShutdownSequencer(subsystem, obsMode)
  )
```

This method creates a **ShutdownSequencer** which is an ADT subtype of a **SequenceManagerRequest**,
the type used by msocket to send over the wire via an HTTP POST. Then the model is sent to the post
client to be transported via msocket.

So what does the testing fixture for this class ([SequenceManagerClientTest]($github.base_url$/esw-sm/esw-sm-api/jvm/src/test/scala/esw/sm/api/client/SequenceManagerClientTest.scala))
look like?  Well, if we follow our strategy, we will use a real implementation of
SequenceManagerClient, and the post client will be mocked.  

```scala
class SequenceManagerClientTest
  extends BaseTestSuite
  with SequenceManagerServiceCodecs {
  
  val postClient: Transport[SequenceManagerRequest] = 
    mock[Transport[SequenceManagerRequest]]
  
  val client = new SequenceManagerClient(postClient)
}
```

To do this, we need to implement mocked behavior for `postClient.requestResponse`. For our method,
this looks like this:

```scala
"SequenceManagerClient" must {
  "return ShutdownSequencersResponse for shutdownSequencer request" in {
    val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
    val shutdownSequencerMsg = ShutdownSequencer(ESW, obsMode, None)
    when(
      postClient.requestResponse[ShutdownSequencersResponse](argsEq(shutdownSequencerMsg))(
        any[Decoder[ShutdownSequencersResponse]](),
        any[Encoder[ShutdownSequencersResponse]]()
      )
    ).thenReturn(Future.successful(shutdownSequencersResponse))

    client.shutdownSequencer(ESW, obsMode, None).futureValue shouldBe shutdownSequencersResponse
  }
}

```

One thing to note is that we are also mocking the response. This insures that the response we get
back from the call is coming from this mock, and not always returned by this method.

So what is the test verifying?  In essence, it is simply showing the the client call is connected to
the proper HTTP request. Since that is all this layer is doing, the test is complete (as long as all
client calls are tested). We do not need to test the values of the **ShutdownSequencer** model
because we are simply testing that the correct request is made.

### SequenceManagerRequestHandler

Now we will move on to the [SequenceManagerRequestHandler]($github.base_url$/esw-sm/esw-sm-handler/src/main/scala/esw/sm/handler/SequenceManagerRequestHandler.scala)
class, which is called in msocket to handle this request. The code for the handler looks like this:

```scala
class SequenceManagerRequestHandler(sequenceManager: SequenceManagerApi, securityDirectives: SecurityDirectives)
    extends HttpPostHandler[SequenceManagerRequest]
    with ServerHttpCodecs {

  import sequenceManager.*
  override def handle(request: SequenceManagerRequest): Route =
    request match {
     case Configure(obsMode) =>
        sPost(complete(configure(obsMode)))
      
      case ShutdownSequencer(subsystem, obsMode, variation) => 
        sPost(complete(shutdownSequencer(subsystem, obsMode, variation)))

      //other ..

    }

  def sPost(route: => Route): Route = securityDirectives.sPost(AuthPolicies.eswUserRolePolicy)(_ => route)
}
```

Here, the lower layer is the **SequenceManagerImpl**, passed in as the `sequenceManager`. For our
method, the `handle` method matches the request model as a **ShutdownSequencer** type, and then
calls `shutdownSequencer(subsystem, obsMode, variation)` which is an imported method of `sequenceManager`
object. This call is wrapped in an `sPost` method, which provides AAS security, and `complete`, is
an Akka-HTTP method to complete the HTTP request.

Again, this layer is just an adapter layer that changes HTTP requests into **SequenceManagerImpl**
calls. All that is needed to test this class is to make sure each request calls the appropriate Akka
client call. The real implementation of this class is used, and the layer below is mocked. `sPost`
is mocked too, since this is already tested as part of AAS:

```scala
class SequenceManagerRequestHandlerTest
    extends BaseTestSuite
    with ScalatestRouteTest
    with SequenceManagerServiceCodecs
    with ClientHttpCodecs {
  private val sequenceManagerApi = mock[SequenceManagerApi]
  private val securityDirectives = mock[SecurityDirectives]
  private val postHandler = new SequenceManagerRequestHandler(sequenceManagerApi, securityDirectives)

  import LabelExtractor.Implicits.default

  private val route = new PostRouteFactory[SequenceManagerRequest]("post-endpoint", postHandler).make()

}
 ```

The test creates a `post-endpoint` route via msocket, just like we using in our msocket-based HTTP
server. Now, our test uses Akka-HTTP to POST a request to this route, using the Akka-HTTP testkit to
`check` that what happens when the request with the proper model is sent to this endpoint is what
expect.  

```scala
"SequenceManagerRequestHandler" must {
    "return shutdown sequencer success for shutdownSequencer request" in {
      when(securityDirectives.sPost(eswUserPolicy)).thenReturn(accessTokenDirective)
      when(sequenceManagerApi.shutdownSequencer(ESW, obsMode, None))
        .thenReturn(Future.successful(ShutdownSequencersResponse.Success))

      Post("/post-endpoint", ShutdownSequencer(ESW, obsMode, None).narrow) ~> route ~> check {
        verify(securityDirectives).sPost(eswUserPolicy)
        verify(sequenceManagerApi).shutdownSequencer(ESW, obsMode, None)
        responseAs[ShutdownSequencersResponse] should ===(ShutdownSequencersResponse.Success)
      }
    }
}
```

This test verifies when we send this model to this endpoint, `sPost` is called, and then the
`shutdownSequencer` method of our Akka client is called, and the response matches the mocked
response set up to return from that client method call. Note, in this case, the response here is a
single real type of expected response, and not a mocked response. This is because the response
requires serialization and we don’t have serialization set up for mocked typed. This is okay because
we have tested serialization for all of our responses with round trip testing.

### Round Trip Testing

As mentioned above, testing of de/serialization for all the public facing models is done using
[RoundTripTest]($github.base_url$/esw-contract/src/test/scala/esw/contract/data/RoundTripTest.scala)

The [esw-contract]($github.dir.base_url$/esw-contract/src/main/scala) module contains examples
for all the models which are sent over the wire for HTTP requests/responses and AKKA messages.
**RoundTripTest** consumes all these models and verifies that they are serialized and deserialized
properly using both `CBOR` and `JSON` formats.

```scala
class RoundTripTest extends AnyFreeSpec with Matchers {

  EswData.services.data.foreach { case (serviceName, service) =>
    s"$serviceName " - {
      "models" - {
        service.models.modelTypes.foreach { modelType =>
          modelType.name - {
            validate(modelType)
          }
        }
      }

      "http requests" - {
        service.`http-contract`.requests.modelTypes.foreach { modelType =>
          validate(modelType)
        }
      }

      "websocket requests" - {
        service.`websocket-contract`.requests.modelTypes.foreach { modelType =>
          validate(modelType)
        }
      }
    }
  }

  private def validate(modelType: ModelType[_]): Unit = {
    modelType.models.zipWithIndex.foreach { case (modelData, index) =>
      s"${modelData.getClass.getSimpleName.stripSuffix("$")}: $index" - {
        List(Json, Cbor).foreach { format =>
          format.toString in {
            RoundTrip.roundTrip(modelData, modelType.codec, format) shouldBe modelData
          }
        }
      }
    }
  }
}

object RoundTrip {
  def roundTrip(modelData: Any, codec: Codec[_], format: Target): Any = {
    val bytes = format.encode(modelData)(codec.encoder.asInstanceOf[Encoder[Any]]).toByteArray
    format.decode(bytes).to[Any](codec.decoder.asInstanceOf[Decoder[Any]]).value
  }
}
```

As seen in code above, the following actions are performed:

- Iterate over all the types of models in `EswData.services.data`, which includes *models* for Akka, *HTTP request models* and
  *websocket request models*,  and call the `validate` method for each of them.
- The `validate` method iterates over all the models of each type and calls the `RoundTrip.roundTrip` method for each of
  them with both `CBOR` and `JSON` format.
- The`roundTrip` method converts the `modelData` to `bytes` and then `bytes` back to the original `modelData`
  using the `codec` and `format` parameters passed in for `CBOR` and `JSON`.

### SequenceManagerImpl

The next layer down is the Akka Client, [SequenceManagerImpl]($github.base_url$/esw-sm/esw-sm-api/jvm/src/main/scala/esw/sm/api/actor/client/SequenceManagerImpl.scala).
It takes API method calls and turns them into Akka ask calls to the Sequence Manager actor. The
lower layer in this case is the Sequence Manager actor, implemented in **SequenceManagerBehavior**.  

In this, however, the actor reference is not passed in. It is assumed to be created (by the wiring)
and registered with the Location Service. Therefore, just the location is passed in and the actor
reference is obtained from that:

```scala
class SequenceManagerImpl(location: AkkaLocation)(implicit actorSystem: ActorSystem[_]) extends SequenceManagerApi {

  private val smRef: ActorRef[SequenceManagerMsg] = location.uri.toActorRef.unsafeUpcast[SequenceManagerMsg]

} 

```

The code for our method simply creates an Akka message the actor can handle (in this case,
**SequencerShutdown**, which is a subtype of **SequenceManagerMsg**), and then performs an Akka ask
by sending this message to the SM actor and transforming the reply into a Future.

```scala
override def shutdownSequencer(
      subsystem: Subsystem,
      obsMode: ObsMode,
      variation: Option[Variation]
  ): Future[ShutdownSequencersResponse] =
    (smRef ? (ShutdownSequencer(subsystem, obsMode, variation, _)))(
      SequenceManagerTimeouts.ShutdownSequencer,
      actorSystem.scheduler
    )
```

For testing, instead of mocking the actor behavior directly, we use the [AskProxyTestKit]($github.base_url$/esw-test-commons/src/main/scala/esw/testcommons/AskProxyTestKit.scala).
This piece of code can be used to create a custom behavior for an Akka ask with a particular message.
In our case, we set up the **AskProxyTestKit** expect messages to be of type **SequenceManagerMsg**
and come from a **SequenceManagerImpl**.

The test kit is set up by overriding the `make` method to return an instance of our Akka client to
be used in the test kit features.

```scala
private val askProxyTestKit = new AskProxyTestKit[SequenceManagerMsg, SequenceManagerImpl] {
    override def make(actorRef: ActorRef[SequenceManagerMsg]): SequenceManagerImpl = {
      val location =
        AkkaLocation(AkkaConnection(ComponentId(Prefix(ESW, "sequence_manager"), Service)), actorRef.toURI, Metadata.empty)
      new SequenceManagerImpl(location)
    }
  }
```

The use of the test kit for our method is shown below, **SequenceManagerImplTest**:

```scala
"SequenceManagerImpl" must {
  "shutdownSequencer" in {
      val shutdownSequencersResponse = mock[ShutdownSequencersResponse]
      withBehavior { case SequenceManagerMsg.ShutdownSequencer(`subsystem`, `obsMode`, `variation`, replyTo) =>
        replyTo ! shutdownSequencersResponse
      } check { sm =>
        sm.shutdownSequencer(subsystem, obsMode, variation).futureValue should ===(shutdownSequencersResponse)
      }
    }
}
```

The `withBehavior` block is used to define whatever behavior is needed to handle the incoming
message, which is a **ShutdownSequencer** message. For testing, we just want it to send a mocked
response. The `check` block is used to execute the command we are testing. The `sm` object in this
lambda is the **SequenceManagerImpl** we created when instantiating the test kit. Here, the check is
making sure when we call the method on the client, our mocked response, which must come from our
mocked behavior, is returned.

### SequenceManagerBehavior

Now we are down in our actor ([SequenceManagerBehavior]($github.base_url$/esw-sm/esw-sm-impl/src/main/scala/esw/sm/impl/core/SequenceManagerBehavior.scala)),
which uses utility classes to carry out operations. For this operation, it requires the
**SequencerUtil** class, which is again created by the wiring and passed into the constructor of the
behavior. Here is a snippet of handling our message:

```scala
 private def idle(self: SelfRef) =
    receive[SequenceManagerIdleMsg](Idle) {
      case Configure(obsMode, replyTo) => configure(obsMode, self, replyTo)
      case Provision(config, replyTo)  => provision(config, self, replyTo)

      // Shutdown sequencers
      case ShutdownSequencer(subsystem, obsMode, variation, replyTo) =>
        sequencerUtil
          .shutdownSequencer(Variation.prefix(subsystem, obsMode, variation))
          .map(self ! ProcessingComplete(_))
          .recoverWithProcessingError[ShutdownSequencer](self)
        processing(self, replyTo)
  }
```

This takes the information in the message and calls the `shutdownSequencer` method in
**SequencerUtil**, and then maps the response to be sent to itself, which now transitions to the
processing state. When the actor receives the response in the `processing` state, it is then returned
to the `replyTo` actor of the original message, and the SM actor returns to the `idle` state.

This means our test fixture needs to verify the following things:

- When the actor receives the **ShutdownSequencer** message, it calls the `shutdownSequencer` method
of sequencerUtil with the `subsystem`,`obsMode` and `variation` from the message.
- The actor then transitions to the processing state.
- The response from our `sequenceUtil.shutdownSequencer` is returned to the replyTo actor in the
**ShutdownSequencer** message.
- The actor return to the `idle` state.

As seen below, this is what our test does.  As we seen before the lower layer
(or layers, in this case), are mocked.

```scala
class SequenceManagerBehaviorTest extends BaseTestSuite with TableDrivenPropertyChecks {

  private val locationServiceUtil: LocationServiceUtil =
    mock[LocationServiceUtil]

  private val agentUtil: AgentUtil = mock[AgentUtil]
  private val sequencerUtil: SequencerUtil = mock[SequencerUtil]

  private val sequenceComponentUtil: SequenceComponentUtil = mock[SequenceComponentUtil]

  private val sequenceComponentAllocator: SequenceComponentAllocator = mock[SequenceComponentAllocator]

  private val sequenceManagerBehavior = new SequenceManagerBehavior(
    config,
    locationServiceUtil,
    agentUtil,
    sequencerUtil,
    sequenceComponentUtil
  )

  "ShutdownSequencer" must {
    val responseProbe = TestProbe[ShutdownSequencersResponse]()
    val prefix = Prefix(ESW, darkNight.name)
    val shutdownMsg = ShutdownSequencer(ESW, darkNight, None, responseProbe.ref)
    s"transition sm from Idle -> Processing -> Idle state and stop| ESW-326, ESW-345, ESW-166, ESW-324, ESW-342, ESW-351, ESW-561" in {
      when(sequencerUtil.shutdownSequencer(prefix)).thenReturn(future(1.seconds, ShutdownSequencersResponse.Success))

      // STATE TRANSITION: Idle -> ShutdownSequencers -> Processing -> Idle
      assertState(Idle)
      smRef ! shutdownMsg
      assertState(Processing)
      assertState(Idle)

      responseProbe.expectMessage(ShutdownSequencersResponse.Success)
      verify(sequencerUtil).shutdownSequencer(prefix)
    }
  }
}

```

### SequencerUtil

Now, we will move down to the next layer, [SequencerUtil]($github.base_url$/esw-sm/esw-sm-impl/src/main/scala/esw/sm/impl/utils/SequencerUtil.scala).
This is where the Sequencer to shutdown is located and commanded to shutdown, which involves finding
the Sequencer, getting the Sequence Component for that Sequencer, and then sending it an
`unloadScript` command. This command is implemented in **SequenceComponentUtil**. The following
shows bits of code from SequenceUtil pieced together to show the relevant logic:

```scala
class SequencerUtil(
  locationServiceUtil: LocationServiceUtil,
  sequenceComponentUtil: SequenceComponentUtil
)(implicit actorSystem: ActorSystem[_]) {

  def shutdownSequencer(prefix: SequencerPrefix): Future[ShutdownSequencersResponse] =
    shutdownSequencersAndHandleErrors(getSequencer(prefix))

  private def getSequencer(prefix: SequencerPrefix): Future[Either[EswLocationError.FindLocationError, List[SeqCompLocation]]] =
    locationServiceUtil.findSequencer(prefix).mapRight(List(_))

  private def shutdownSequencersAndHandleErrors(
  sequencers: Future[Either[EswLocationError, List[AkkaLocation]]]
  ) =
    sequencers
    .flatMapRight(unloadScripts)
    .mapToAdt(identity, locationErrorToShutdownSequencersResponse)

    // get sequence component from Sequencer and unload sequencer script
  private def unloadScript(sequencerLocation: AkkaLocation) =
    makeSequencerClient(sequencerLocation).getSequenceComponent
      .flatMap(sequenceComponentUtil.unloadScript)
      .map(_ => ShutdownSequencersResponse.Success)

  private def unloadScripts(sequencerLocations: List[AkkaLocation]) =
    Future.traverse(sequencerLocations)(unloadScript).map(_ => ShutdownSequencersResponse.Success)

  // Created in order to mock the behavior of sequencer API availability for unit test
  private[sm] def makeSequencerClient(sequencerLocation: Location): SequencerApi = 
    SequencerApiFactory.make(sequencerLocation)
}
```

Note that the Location Service is used to get a **SequencerApi** reference, which is used to the
location of the Sequence Component.  Then, **SequenceComponentUtil** is used to shutdown the
Sequencer using the SequenceComponent location.

The Location Service and the SequenceComponentUtil classes are the lower layers in this class, so
they are passed in, and mocked when testing.  

### SequenceComponentUtil

The Location Service is a separate service, so it is tested separately. The
[SequenceComponentUtil]($github.base_url$/esw-sm/esw-sm-impl/src/main/scala/esw/sm/impl/utils/SequenceComponentUtil.scala)
is the next layer of the Sequence Manager, and therefore requires tests. For our method, this class
uses the Sequence Component location to unload the script, thus destroying the Sequencer:

```scala
class SequenceComponentUtil(
  locationServiceUtil: LocationServiceUtil,
  sequenceComponentAllocator: SequenceComponentAllocator
)(implicit actorSystem: ActorSystem[_]) {

  def unloadScript(seqCompLocation: SeqCompLocation): Future[Ok.type] = 
    sequenceComponentApi(seqCompLocation).unloadScript()


  private[sm] def sequenceComponentApi(
  seqCompLocation: SeqCompLocation
  ): SequenceComponentApi = new SequenceComponentImpl(seqCompLocation)
}
```

This is the bottom layer of our method. For our method here, a new Akka client to the Sequence
Component is constructed from the location, and the `unloadScript` method is called on it. Thus, we
have reached the bottom of our layers, and will not need to provide any test classes beyond this.
The testing for this class must have tests for the methods we use, which in this case is just
`unloadScript`.

```scala
"unloadScript" must {
    val mockSeqCompApi = mock[SequenceComponentApi]

    val sequenceComponentUtil = 
      new SequenceComponentUtil(
      locationServiceUtil, 
      sequenceComponentAllocator
      ) {
      override private[sm] def sequenceComponentApi(
      seqCompLocation: AkkaLocation
      ): SequenceComponentApi = mockSeqCompApi
    }

    "return Ok if unload script is successful" in {
      val seqCompLocation = sequenceComponentLocation("esw.primary")
      when(mockSeqCompApi.unloadScript()).thenReturn(Future.successful(Ok))

      sequenceComponentUtil.unloadScript(seqCompLocation).futureValue should ===(Ok)

      verify(mockSeqCompApi).unloadScript()
    }
  }

```

Here, the Akka client for the Sequence Component is mocked, and it’s shown that when the
`unloadScript` method in **SequenceComponentUtil** is called, it calls the `unloadScript` method in
the Akka client. Note that this method always returns **Ok**. If it didn’t, the failure modes of
this call would also need to be tested, using mocks, and it must be verified the proper error
response is returned.

### Additional Testing

This leads us to discuss additional testing. It is important that all code paths are tested at at
least one layer. The best place to test this is at the lowest level they occur. Let’s go back at
take a look at **SequenceUtil**.  

It uses the Location Service to find a reference to the Sequencer. There are two ways this operation
can fail. If the Sequence is not registered, it can be assumed to not be running, and this is
considered a success, since the goal of the method is to shutdown the Sequencer anyway. Another
failure mode is if an error occurs when getting the location, such as the Location Service isn’t
running or working properly. This would result in a **RegistrationListingFailed**. Since these two
alternate scenarios represent cases in which the logic for the command come to an end, they are
shown in the testing for this class:

```scala
 "shutdownSequencer" must {
    "return Success even if sequencer is not running" in {
      // mimic the exception thrown from LocationServiceUtil.findSequencer
      val findLocationFailed = futureLeft(LocationNotFound("location service error"))

      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix))
      .thenReturn(findLocationFailed)

      sequencerUtil.shutdownSequencer(eswDarkNightSequencerPrefix).futureValue should ===(
      ShutdownSequencersResponse.Success)

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
      verify(eswSequencerApi, never).getSequenceComponent
    }

    "return Failure response when location service returns RegistrationListingFailed error" in {
      when(locationServiceUtil.findSequencer(eswDarkNightSequencerPrefix))
        .thenReturn(futureLeft(RegistrationListingFailed("Error")))

      sequencerUtil.shutdownSequencer(eswDarkNightSequencerPrefix).futureValue should ===(
      LocationServiceError("Error"))

      verify(locationServiceUtil).findSequencer(eswDarkNightSequencerPrefix)
    }
  }

```

If we take another look at the **SequencerUtil** class, we can see that when this Location Service
error occurs, it’s actually transformed to another type:

```scala
 private def shutdownSequencersAndHandleErrors(sequencers: Future[Either[EswLocationError, List[AkkaLocation]]]) =
    sequencers.flatMapRight(unloadScripts).mapToAdt(identity, locationErrorToShutdownSequencersResponse)

  private def locationErrorToShutdownSequencersResponse(err: EswLocationError) =
    err match {
      case _: EswLocationError.LocationNotFound => ShutdownSequencersResponse.Success
      case e: EswLocationError                  => LocationServiceError(e.msg)
    }
```

The **LocationServiceError** type is a **ShutdownSequencersResponse.Failure** message that can be
returned to the Akka client. Therefore, this type needs to be tested in the
**SequenceManagerBehaviorTest**

```scala
"ShutdownSequencer" must {
    val responseProbe = TestProbe[ShutdownSequencersResponse]()
    val prefix        = Prefix(ESW, darkNight.name)
    val shutdownMsg   = ShutdownSequencer(ESW, darkNight, None, responseProbe.ref)

   s"return LocationServiceError if location service fails" in {
      val err = LocationServiceError("error")
      when(sequencerUtil.shutdownSequencer(prefix)).thenReturn(Future.successful(err))

      smRef ! shutdownMsg
      responseProbe.expectMessage(err)

      verify(sequencerUtil).shutdownSequencer(prefix)
    }
}
```

The `sequencerUtil` method is mocked to return the error response, and it is verified that if the
actor receives the shutdown message, this is returned to the `replyTo`.

## Component Tests

For completeness, a component test should be added. Component tests should not use any mocks.
This test should involve methods that go up and down the chain, but not all methods need to be
included, nor all code paths for the methods used. That is what the units tests are for. However,
for completeness, there should be a component test for every public method in outward facing APIs
showing at least one code path.

Following example demonstrates how to write a component test for the Sequence Manager's
`configure` and `shutdownObsModeSequencers` APIs.  

@@@ note
The class name for the tests in our example have the words "integration test" in it, but this is a bit of a misnomer.
While the tests are performed on the *integrated* component, it should not be confused with the 
integration tests as described in the TMT Software Development Process, which involve multiple components.
The name of these types of tests will be changed in subsequent release to avoid confusion.
@@@

```scala
class SequenceManagerSossIntegrationTest extends EswTestKit(EventServer) {

  "SOSS" must {
    "have ability be able to spawn sequencer hierarchy and send sequence to top level sequencer" in {
      // ======= Setup =======
      val sequenceManagerPrefix = Prefix(ESW, "sequence_manager")
      val obsMode               = ObsMode("IRIS_Cal")

      val sequence = Sequence(
        Setup(sequenceManagerPrefix, CommandName("command-1"), None)
      )
      
      // Start required sequence components
      TestSetup.spawnSequenceComponent(ESW, None)
      TestSetup.spawnSequenceComponent(AOESW, None)
      TestSetup.spawnSequenceComponent(IRIS, None)

      // Start Sequence Manager
      val sequenceManager = TestSetup.startSequenceManager(sequenceManagerPrefix)

      // ======= Verification =======
      // configure obsMode
      val configureResponse = sequenceManager.configure(obsMode).futureValue
      configureResponse should ===(
        ConfigureResponse.Success(
          ComponentId(Prefix(ESW, obsMode.name), Sequencer)
        )
      )
      
      val successResponse = configureResponse.asInstanceOf[Success]
      val id              = successResponse.masterSequencerComponentId
      val location        = resolveHTTPLocation(id.prefix, id.componentType)

      SequencerApiFactory
        .make(location)
        .submitAndWait(sequence) // Submit sequence to top level sequencer
        .futureValue shouldBe a[Completed]

      // ======= Cleanup =======
      sequenceManager
        .shutdownObsModeSequencers(obsMode)
        .futureValue shouldBe a[ShutdownSequencersResponse.Success.type]
    }
  }
}
```

There are a few things to note about this test. [TestSetup]($github.base_url$/esw-integration-test/src/test/scala/esw/sm/app/TestSetup.scala)
is a utility class which supports starting real Sequence Component and Sequence Manager without
mocks. It just delegates call to underlying Sequencer or Sequence Manager application/wiring.

[SequenceManagerSossIntegrationTest]($github.base_url$/esw-integration-test/src/test/scala/esw/sm/app/SequenceManagerSossIntegrationTest.scala)
demonstrates starting `IRIS_Cal` observation mode, submitting `sequence` to the top level sequencer
and then stopping the observation.

This component test is divided into following three parts:

### Setup

- Start required Sequence Components that are `ESW`, `AOESW` and `IRIS` for the `IRIS_Cal`
  observation using `TestSetup.spawnSequenceComponent` method.
- Start Sequence Manager using the `TestSetup.startSequenceManager(sequenceManagerPrefix)` method.

### Verification

- Submit the `configure` command to the Sequence Manager and verify a successful configure response is returned.
- Resolve the location of the top level sequencer and create a **SequencerApi** using that location.
- Submit a `sequence` using the `sequencerApi.submitAndWait(sequence)` method and verify it returns
  a **Completed** response

### Cleanup

- Shutdown all the sequencers that are started while configuring `IRIS_Cal` observation using
  `sequenceManager.shutdownObsModeSequencers(obsMode)` method.
