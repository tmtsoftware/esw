# Sequencers

Sequencers all use the same component framework. What makes a Sequencer unique is the Script it is loaded with. 
A Sequencer is a Sequence Component configured with a specific Script. The Script is written with a specific observing mode 
(or set of common observing modes) in mind. The observing modes a Script can support is up to the developer, but the intention is that a Script can be developed independently of other Scripts to refine behavior specific to an observing mode, without affecting any other observing modes. Since the Script defines the behavior of the Sequencer, one can be written to support a simulation mode or a standalone mode, such that development and testing can be performed with a Sequencer handling real Sequences, but only simulating its actions.
In order to provide thread-safe concurrency, the Active Object design pattern is used for Scripts. The Active Object design pattern features a single “Executor” thread, in which all requests are sent to, such that only one request is processed at a time. This allows the Script to maintain global variables that can be accessed in a thread-safe way.


The main class application for Sequencers is part of the Script repository. By executing an sbt stage command, all scripts and
the application executable are written as compiled class files to a stage directory. Executing the application in the stage directory 
will allow the Sequencer application to locate any necessary Scripts. The sbt tool is used to start the application, which will bundle any 
necessary dependencies, including the Sequencer framework.

This application takes the Sequencer name as an argument and sets up the initial SequencerComponentWiring. 
In this wiring, the environment is set up in an object called CswSystem. This creates the top level ActorSystem 
and provides access this system through some methods which simplify the creation of other actors in the Sequencer Component. 
It also creates a GuardianActor, which serves a watchdog over the other actors in the system and allow for graceful shutdown 
of those actors when the system is shutting down.
 

The SequencerComponentWiring then creates a SequencerComponent actor, whose purpose is to allow the loading of scripts dynamically. 
The SequencerComponent starts in the “idle” state. in which the only acceptable message it can receive is LoadScript message. 
When this is received, the core of the Sequence Component is constructed using an object called Wiring. 
This include its Akka Actor System, Supervisor, Engine, CSW Services access, utilities (e.g. CRM), and clients, and the remote access system 
that allows REPL control of the Sequencer. The specified script is loaded from disk, and the 
SequencerComponent actor then changes to the “running” state.
When the SequencerComponent is constructed, it is registered in the Location Service using the Sequencer name passed into the application. 
This allows external entities to locate this actor and send it the LoadScript message. When a script is loaded, the Wiring registers the 
Supervisor actor with the Location Service separately using the name specific to the script loaded. This allows Sequencer 
commands (e.g. submit, start, pause, resume, etc.) to be sent directly to the Supervisor. Note that the wiring is specific to the script. 
When a StopScript message is sent to the SequencerComponent actor, the wiring is torn down, and the SequencerComponent returns to the idle state.
The LoadScript message indicates the new observing mode for the Sequencer. The application will load the observing mode 
to Script map, which is also stored in the Script Service, to identify the appropriate Script class, which is loaded using the Java class loader by reflection.