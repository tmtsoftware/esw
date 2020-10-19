# ESW Gateway
Gateway gives access to all CSW and ESW services (available to user) and components from browser-based user interfaces.
Gateway also enforces auth on command and sequencer APIs to protect from unauthorised access.

![ESW Gateway](../../images/gateway/gateway.svg)

# Implementation

Gateway service is an HTTP service which relies on `msocket` framework. Internally it delegates to the various 
services(of both ESW or CSW) needed as per the request.

It provides access to following:

* Command Service APIs (CSW)
* Alarm APIs (CSW)
* Event APIs (CSW)
* Sequencer Service APIs (ESW)
* Admin APIs (ESW)
* Logging APIS (ESW)

Some services are not part of the gateway. This is mainly because of 2 reasons:
1. The services can be accessed via its own HTTP interface. Example, Config Service.
2. The services are not user facing. Example, Sequence Manager Service.

The security directives created in the app for command and sequencer services are passed on to the routes in order to 
enable auth for command and sequencer services.
 
# Modules

### esw-gateway-api

All the request models, clients, and APIs exposed in gateway service resides within this module. 
This also contains the codecs for the models. 

It is a cross-compiled project which has following parts:

- js - code used by scala-js.
- jvm - code used by jvm
- shared - code which can be used by both scala-js and jvm

### esw-gateway-impl

This module depends on `esw-gateway-api` and contains implementation for all the apis declared in `esw-gateway-api`.
In case of CSW services(ex, Event Service, Alarm Service), the implementation delegates the call to respective CSW service. 

### esw-gateway-server

This module contains the main application which when run starts the gateway server. This includes server wiring, 
post and websocket handlers and cli app.

This module depends on `esw-http-core` for starting up an HTTP service.    
