# sequence-manager-app

A command line application that facilitates starting Sequence Manager and HTTP server of Sequence Manager.

## Supported Commands

* start

Starts sequence manager and http server of sequence manager.

Options accepted by this command are described below:

 * `-o` : Config file path which has mapping of sequencers and resources needed for different observing modes
 * `-l` : optional aregument (true if config is to be read locally or false if from remote server) default value is false
 * `-a` : optional argument: agentPrefix on which sequence manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc.
          This argument is used when Sequence Manager is spawned using Agent. For starting standalone sequence manager for testing or on local
          this argument is not needed.


This command starts Sequence Manager as well as HTTP server of Sequence Manager.

@@@notes
In location service, registration for ESW.sequence_manager as Akka registration as well as HTTP registration will be seen.
HTTP server of Sequence Manager exposes endpoints to interact with sequence manager like provision, configure observing mode etc.
@@@


### Examples:

```
esw-sm-app start -o obsmode.conf -l
```

```
esw-sm-app start -o obsmode.conf
```


### Setting the default log level

The default log level for any component is specified in the `application.conf` file of the component.  In this case,
the Sequence Component is shared code among all Sequencers.  Therefore, to specify a log level for your Sequencer,
use the java -D option to override configuration values at runtime.  For log level, the format is:

```
-Dcsw-logging.component-log-levels.<Subsystem>.<obsMode>=<LEVEL>
```

For example, using the example above:

```
esw-sm-app start -o obsmode.conf -l -Dcsw-logging.component-log-levels.ESW.sequence_manager=TRACE
```
