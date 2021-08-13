# Parts of a esw application

This sectiion describes parts of any esw application.

Main class
 This class is the starting point of any application, Here we specify what arguments the application accepts. Main class extends `CommandApp` which is a `case app`, it is a utility to capture program arguments in command model class. `Command` class has fields corresponding to each supported program argument. For esw applications, we use a rich wrapper class `EswCommandApp` build on top of `CommandApp`.
 When we run Main class it creates a new instance `Wiring` class and triggers Http server startup. How to start Http server and what other things to initialize is decided by `Wiring` class.
e.g.
For Gateway service application refer classes `GatewayMain` and `ServerCommand`
For Agent service application refer `AgentServiceApp`, `AgentServiceAppCommand`

Wiring class
  This class takes care of initializating instance of various other classed required by this application like `ActorSystem`, api instances, setting overrides, `HttpService` , Handlers for routes and clients to connect to other services like location service client, event service client, security etc.
e.g `GatewayWiring`, `AgentServiceWiring`, `SequenceManagerWiring`

HttpService
 Most of esw applications needs a HTTP server to handle http requests, Hence we have extracted to a common module `esw-http-core`, which has things common to all esw http applications. `HttpService` class is responsible for initializing a HTTP Server and registering its location to location service.
 Module `esw-http-core` has other common classes like `ActorRuntime` wrapper containing actor related class refences, `Settings` containing logic to extract common settings.

Route handlers
  There are two type of route handlers.
  PostHandler - This handler contain routes corresponding to http requests. e.g. `GatewayPostHandler`, `AgentServicePostHandler`. Since the underlying infrastructure used to handle Htpp requests is using Msocket library, all requests are of type `POST`.
  WebsocketHandler - This handler contain routes corresponding to web-socket requests. e.g. `GatewayWebsocketHandler`, `SequencerWebsocketHandler`.
Since, websocket request work on top of http protocol, routes in WebsocketHandler are handled by `HttpService` along with PostHandler reoutes.

Api, Impl and Behavior classes
  Api - These classes define the contracts for external users of our application. e.g. `SequencerApi`, `SequenceManagerApi`, `AlarmApi`.
  Impl - These classes has implementation to Api classes. e.g. `SequencerImpl`, `SequenceManagerImpl`, `AlarmImpl`. These classes can call other services to fullfil the requests or handle it using corresponding Behaviour classes.
  Behaviour - Impl classes dont have logic to manage application state of the system, it is handled by Behavious classes. They use Akka Actors state machine pattern to manage state. e.g. `SequencerBehavior`, `SequenceManagerBehavior`.

Codecs
 When you send request over the wire, it needs to be converted and send in some standard format like JSON. For this purpose we use Codec classes which use Borer library to automatically derive JSON and CBOR format. e.g. in `OcsCodecs` `implicit lazy val stepCodec: Codec[Step] = deriveCodec`, here deriveCode do the job of generating decoder/encoder for serialization/deserialization. These Codec classes contain reference of model case classes and these are marked implicit, so that when a class extends this codec class it gets the model class codec automatically. e.g `OcsCodecs`, `SequencerServiceCodecs`. You can even have heirarchy of model classes and you just need to provide top level class/marker trait and it will automatically derive child class codecs. e.g. in `SequencerServiceCodecs` `implicit lazy val sequencerPostRequestValue: Codec[SequencerRequest]  = deriveAllCodecs` , here you see deriveAllCodecs which means generate decoder/encoder of complete class heirarchy starting from top level `SequencerRequest`.
 There are mainly two use cases when you need to serialize/deserialize of request and response.
  Remote Actor Communication - First use case is when your actor system interact with other remote actor systems from other services. We use cbor format for serialization/deserialization in our application for it. e.g. When Sequencer receive requests from Gateway via Actors. For such cases first we need to add model classes to Codecs so that encoder/decoder can be derived. e.g. `AgentActorCodecs`. Then we need to registed models classes to serializer class e.g. `AgentAkkaSerializer` and also we need to mark our model classes as Serializable e.g. `sealed trait AgentResponse extends AgentAkkaSerializable`. Both Serializer and Serializable class are present in configuration of application so that Akka Actors can use them for serialization/deserialization. e.g. in `esw-agent-akka-client` module `reference.conf` has `AgentAkkaSerializer` as serializers and `AgentAkkaSerializable` as serialization-bindings.
  Http Communication - Second use case is when consume your request and send response back over Http protocol. For such use cases, when a request goes to PostHandler, it need a mechanism to deserialize request, process it and serialize the response and sent it back. For this reason PostHandler uses HttpCodec classes which takes care of serialization and deserialization. Since, Web socket protocol work on top of Http protocol hence WebsocketHandler uses same mechanism as PostHandler. e.g. `SequencerWiring` extends `SequencerServiceCodecs` and further `SequencerWiring` pass Codecs to `SequencerPostHandler` via `PostRouteFactory` and to `SequencerWebsocketHandler` via `WebsocketRouteFactory`.

Complete flow of above discussed classes is :
 Main (using case app) -> Wiring -> HttpService(esw-http-core) -> HttpPostHandler(using Codecs) + WebsocketHandler(using Codecs) -> Behaviour classes(using Codecs).
