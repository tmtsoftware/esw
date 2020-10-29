# Starting Sequence Manager Using sequence-manager

`sequence-manager` is a command line application that facilitates starting Sequence Manager
and the HTTP server that is part of Sequence Manager using the `coursier` dependency management
application. The `coursier` tool is described with full documentation
at the [coursier site](https://get-coursier.io).

## Prerequisites for Running sequence-manager App

The following steps should be followed to use sequence-manager to start a Sequencer Manager.

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Start Any Needed CSW Services

* To run sequence-manager, the **CSW Location Service** must be running.
*  CSW AAS should be running.

Information on starting CSW services is @extref[here](csw:commons/apps)

## 3. Install sequence-manager

The following command creates an executable file named `sequence-manager` in the default installation directory.

```bash
cs install sequence-manager:<version | SHA>
```

One can specify installation directory like the following:

```bash
cs install \
    --install-dir /tmt/apps \
    sequence-manager:<version | SHA>
```
@@@note
If you don't provide the version or SHA in above command, `sequence-manager` will be installed with the latest tagged binary of `esw-sm-app`
@@@

## 4. Run sequence-manager

Once sequence-manager is installed, one can simply run sequence-manager by executing start command

Start command supports following arguments:

- `-o` : Config file path which has mapping of sequencers and resources needed for different observing modes
- `-l` : optional aregument (true if config is to be read locally or false if from remote server) default value is false
- `-a` : optional argument: agentPrefix on which sequence manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc.
          This argument is used when Sequence Manager is spawned using Agent. For starting standalone sequence manager for testing or on local
          this argument is not needed.

This command starts Sequence Manager as well as its HTTP server.

```bash
//cd to installation directory
cd /tmt/apps

// run sequence manager
./sequence-manager start -o obsmode.conf
```

@@@note
Refer to supported arguments section or `./sequence-manager start --help` for starting sequence manager with specific arguments
@@@

## Setting the Log Level

The default log level for any component is specified in the `application.conf` file of the component.
Use the java -J-D option to override configuration values at runtime.  For log level, the format is:

```
-J-Dcsw-logging.component-log-levels.<Subsystem>.<ComponentName>=<LEVEL>
```

For example, using the example above:

```bash
//cd to installation directory
cd /tmt/apps

// run sequence manager
./sequence-manager -J-Dcsw-logging.component-log-levels.ESW.sequence_manager=TRACE start -o obsmode.conf
```
