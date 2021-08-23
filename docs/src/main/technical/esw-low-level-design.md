# ESW Low Level Design

Goal of this section is to provide walk through of the low level design of the ESW applications.
This section is targeted towards future maintainers of the ESW. It should help them understand different parts of the ESW applications and easily navigate through the codebase.

ESW applications are divided into following three major categories:

1. Actor based, for example, Agent Akka Application.
1. HTTP based, for example, Agent Service, Gateway Application.
1. Embedded Http + Actor based - Such applications exposes two protocols for communication, one is HTTP and other is Akka. for example, ESW OCS (Sequencer) Application.

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
Main class creates instance of `Wiring` class and initializes actors and HTTP server wherever applicable.

Examples of Main class for different application modules are,

1. For Gateway Service application, refer classes `GatewayMain` and `ServerCommand`
1. For Agent Service application, refer `AgentServiceApp`, `AgentServiceAppCommand`

## Wiring Class

Wiring class initializes all the dependencies and resources required for creating Actors and HTTP server.
Some of the common examples of instances and resources created by `Wiring` are:
`ActorSystem`, API implementation instances, Application Settings, `HttpService`, HTTP and Websocket handlers and clients to connect to other services like Location Service, Event Service etc.

Examples of `Wiring` class are, `GatewayWiring`, `AgentServiceWiring`, `SequenceManagerWiring` etc.

## HttpService

As mentioned in the introductory section, many ESW applications for example,
Gateway, ocs-app (Sequencer), Sequence Manager etc. are either HTTP based or Embedded HTTP + Actor based,
and requires capabilities to start and register with Location Service.

We have extracted out these common capabilities in independent module called `esw-http-core`.
This module has class `HttpService` which is responsible for initializing a HTTP Server and registering it with Location Service.

`esw-http-core` module has other common utilities like,

1. `ActorRuntime` - Small wrapper over `ActorSystem` responsible for providing implicit `ActorSystem`, `ExecutionContext`, starting Logging Service and closing resources on shutdown.
1. `Settings` - Reading application specific configuration like service prefix, HttpConnection (Used to register with Location Service) etc.

## HTTP and Websocket Handlers

All the HTTP based applications implements `PostHandler` and `WebsocketHandler` traits provided by [msocket](https://github.com/tmtsoftware/msocket) library.

1. PostHandler - This handler contains routes corresponding to http requests. e.g. `GatewayPostHandler`, `AgentServicePostHandler`.
Since the underlying infrastructure used to handle Http requests is using Msocket library, all requests are of type `POST`.

1. WebsocketHandler - This handler contains routes corresponding to web-socket requests. e.g. `GatewayWebsocketHandler`, `SequencerWebsocketHandler`.
Since, websocket request work on top of http protocol, routes in WebsocketHandler are handled by `HttpService` along with PostHandler routes.

## Api, Impl and Actor Behavior Classes

### Api

These classes define the contracts for external users of our application. e.g. `SequencerApi`, `SequenceManagerApi` etc.

### Impl

Impl class is present at server side and implements API (service contract). In most of the cases,
these implementations just sends local message to underlying actor which consist of all the business logic or
call other services to fulfil requests. e.g. `SequencerImpl`, `SequenceManagerImpl` etc.

### Actor Behavior

Most of the business logic and functionality resides within the actor. Actors are responsible for managing application state and implementing requirements specific state machines.
e.g. `SequencerBehavior`, `SequenceManagerBehavior`.

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
