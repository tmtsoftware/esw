# ESW Low Level Design

Goal of this section is to provide walk through of the low level design of the ESW applications.
This section is targeted towards future maintainers of the ESW. It should help them understand different parts of the ESW applications and easily navigate through the codebase.

ESW applications are divided into following two major categories:

1. Pure Actor based, for example, ESW Agent Akka Application.
1. Embedded Http Server based - Such applications exposes two protocols for communication, one is HTTP and other is Akka. for example, ESW Gateway.

Most if not all the ESW applications follows the following conventions for organizing the codebase:

1. Main - Runnable application responsible for starting the ESW service.
1. Wiring - Initializes all the dependencies and wires them together.
1. HttpService - Starts and registers the HTTP service with Location Service.
1. Route Handlers - Defines two types of the routes for the HTTP service i.e. HTTP routes and Websocket routes.
1. API - Interface defining the contract for the Service
   - Impl - Server side implementation of the API.
   - Clients - Two types of clients implementing API i.e. HTTP and Actor clients.
1. Actor Behavior - Defines the behavior of the Service. This includes actual implementation of backend service logic.
1. Codecs - Encoders and Decoders required for the remote communication. This includes codecs required for Actors and HTTP service.

## Main Class

Main class is the entry point for all the ESW applications, and responsible for specifying the command line arguments, options and parsing them to domain models.
This capability is provided by extending Main class with `CommandApp` which is coming from a [case-app](https://github.com/alexarchambault/case-app).
**case-app** is a command line argument parsing library for scala
`Command` class has fields corresponding to each supported program argument.
For ESW applications, we use a rich wrapper class `EswCommandApp` build on top of `CommandApp`.
When we run Main class it creates a new instance `Wiring` class and triggers Http server startup.
How to start Http server and what other things to initialize is decided by `Wiring` class. For example,

1. For Gateway Service application, refer classes `GatewayMain` and `ServerCommand`
1. For Agent Service application, refer `AgentServiceApp`, `AgentServiceAppCommand`kk

## Wiring Class

This class takes care of initializing instances of various other classes required by this application like `ActorSystem`, api instances, setting overrides, `HttpService`, handlers for routes and clients to connect to other services like Location Service, Event Service, security etc.
e.g `GatewayWiring`, `AgentServiceWiring`, `SequenceManagerWiring`

## HttpService

Most of esw applications needs a HTTP server to handle http requests, Hence we have extracted to a common module `esw-http-core`,
which has things common to all esw http applications. `HttpService` class is responsible for initializing a HTTP Server and registering its location to Location Service.
Module `esw-http-core` has other common classes like `ActorRuntime` wrapper containing actor related class references, `Settings` containing logic to extract common settings.

## Route handlers

There are two type of route handlers.

1. PostHandler - This handler contains routes corresponding to http requests. e.g. `GatewayPostHandler`, `AgentServicePostHandler`.
Since the underlying infrastructure used to handle Http requests is using Msocket library, all requests are of type `POST`.

1. WebsocketHandler - This handler contains routes corresponding to web-socket requests. e.g. `GatewayWebsocketHandler`, `SequencerWebsocketHandler`.
Since, websocket request work on top of http protocol, routes in WebsocketHandler are handled by `HttpService` along with PostHandler routes.

## Api, Impl and Behavior classes

### Api

These classes define the contracts for external users of our application. e.g. `SequencerApi`, `SequenceManagerApi`, `AlarmApi`.

### Impl

These classes has implementation to Api classes. e.g. `SequencerImpl`, `SequenceManagerImpl`, `AlarmImpl`.
These classes can call other services to fulfil the requests or handle it using corresponding behaviour classes.

### Behaviour

Impl classes don't have logic to manage application state of the system, it is handled by behaviours classes. They use Akka Actors state machine pattern to manage state. e.g. `SequencerBehavior`, `SequenceManagerBehavior`.

## Codecs

When you send request over the wire, it needs to be converted and send in some standard format like JSON.
For this purpose we use Codec classes which use Borer library to automatically derive JSON and CBOR format.
e.g. in `OcsCodecs` `implicit lazy val stepCodec: Codec[Step] = deriveCodec`, here deriveCode do the job of generating decoder/encoder for serialization/deserialization.
These Codec classes contain reference of model case classes and these are marked implicit, so that when a class extends this codec class it gets the model class codec automatically. e.g `OcsCodecs`, `SequencerServiceCodecs`.
You can even have hierarchy of model classes, and you just need to provide top level class/marker trait, and it will automatically derive child class codecs.
e.g. in `SequencerServiceCodecs` `implicit lazy val sequencerPostRequestValue: Codec[SequencerRequest] = deriveAllCodecs`,
here you see deriveAllCodecs which means generate decoder/encoder of complete class hierarchy starting from top level `SequencerRequest`.
There are mainly two use cases when you need to serialize/deserialize of request and response.

Remote Actor Communication - First use case is when your actor system interact with other remote actor systems from other services.
We use cbor format for serialization/deserialization in our application for it. e.g. When Sequencer receive requests from Gateway via Actors.
For such cases first we need to add model classes to Codecs so that encoder/decoder can be derived.
e.g. `AgentActorCodecs`. Then we need to registered models classes to serializer class e.g. `AgentAkkaSerializer` and also we need to mark our model classes as Serializable e.g. `sealed trait AgentResponse extends AgentAkkaSerializable`.
Both Serializer and Serializable class are present in configuration of application so that Akka Actors can use them for serialization/deserialization.
e.g. in `esw-agent-akka-client` module `reference.conf` has `AgentAkkaSerializer` as serializers and `AgentAkkaSerializable` as serialization-bindings.

Http Communication - Second use case is when consume your request and send response back over Http protocol.
For such use cases, when a request goes to PostHandler, it needs a mechanism to deserialize request, process it and serialize the response and sent it back.
For this reason PostHandler uses HttpCodec classes which takes care of serialization and deserialization.
Since, Web socket protocol work on top of Http protocol hence WebsocketHandler uses same mechanism as PostHandler. e.g. `SequencerWiring` extends `SequencerServiceCodecs` and further `SequencerWiring` pass Codecs to `SequencerPostHandler` via `PostRouteFactory` and to `SequencerWebsocketHandler` via `WebsocketRouteFactory`.

## Complete flow of above discussed classes is

 Main (using case app) -> Wiring -> HttpService(esw-http-core) -> HttpPostHandler(using Codecs) + WebsocketHandler(using Codecs) -> Behaviour classes(using Codecs).
