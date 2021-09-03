# Using the Gateway-only UI Template and Tutorial

A Gateway-only User Interface is a TMT user interface that only needs to use the CSW services to implement its purpose.  Not all
CSW services require the User Inteface Gateway or UIAG, but the UIAG is the backend for most of the UIs functionality.  A TypeScript library
has been created to facilitate using the UIAG and CSW services named ESW-TS. It is documented [here](http://tmtsoftware.github.io/esw-ts/).

Note that not all service functions are provided in ESW-TS. ESW-TS provides the service functionality that makes sense in a User Interface. The
capabilities a Gateway-only UI are summarized here:

**Authentication and Authorization Service.** In the TMT Software System, all users must be authenticated and once
authenticated, their authorized roles are used to determine what commands a user can execute. A Gateway-only UI must
first authenticate users.  ESW-TS helps with this, and the tutorial is an example. 

**Framework.** A UI can create and consume parameters that are part of commands or events. ESW-TS provides functionality to help
with this.

**Command Service.** A Gateway-only UI can send commands to Assemblies, HCDs, and Sequencers and monitor completion using Command Service. ESW-TS provides APIs
for: validate, submit, submitAndWait, submitAllAndWait, oneway, query, queryFinal, and subscribeCurrentState. The tutorial demonstrates
how to send a command.

**Event Service.** Event Service is probably the most important for a Gateway-UI since user interfaces in the observatory are the way users
monitor the state of the systems. A Gateway-UI can use the following Event Service functions: publish, get, subscribe.

**Logging.** Logging is a challenge in the browser-based UI.  The Logging Service provides a log API that goes through to the UIAGLogging Service API so 
that the log messages can be integrated with other log messages in the system.

**Configuration Service.** The Configuration Service is available outside the UIAG so that a UI can access configuration information without going
through the gateway. UI clients must authenticate directly with the Configuration Service.

**Location Service.** Location Service is available outside the UIAG so that a Gateway-only UI (and others) can locate services such as UIAG or a web application backend.

**Sequencer Access.** A client Gateway-only UI can send Sequences to a Sequencer through the UIAG. This is not a normal function for most UI applications, but it 
might be useful during development or testing.

### User Interface Application Gateway (UIAG)
If you are writing a Gateway-only UI, you will need to start and potentially configure the UIAG. The UIAG is described @ref:[here](gateway.md) 
along with the AAS authentication and authorization model. The UIAG application itself is described @ref:[here](gateway-app.md). 

### Gateway-only Template and Tutorial

A template Giter8 project has been provided that should be used to create a Gateway-only user TMT interface.
Please follow the instructions in the README to get started with the template. The repository is located @link:[here](https://github.com/tmtsoftware/esw-ui-template.g8) { open=new }.

A tutorial has also been created that shows the basics of creating and coding a Gateway-only UI using the template.
The tutorial is located @link:[here](https://github.com/tmtsoftware/esw-ui-example).  *** MUST FIX LINK ***