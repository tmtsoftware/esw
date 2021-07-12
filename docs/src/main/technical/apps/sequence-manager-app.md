# Starting Sequence Manager Using esw-sm-app

`esw-sm-app` is a command line application that facilitates starting Sequence Manager, and the HTTP server that is part of Sequence Manager using the `coursier` dependency management
application. The `coursier` tool is described with full documentation
at the [coursier site](https://get-coursier.io).

## Prerequisites for Running esw-sm-app App

The following steps should be followed to use esw-sm-app to start a Sequencer Manager.

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Start Any Needed CSW Services

* To run esw-sm-app, the **CSW Location Service** must be running.
*  CSW AAS should be running.

Information on starting CSW services is @extref[here](csw:commons/apps)

## 3. Install esw-sm-app

The following command creates an executable file named `esw-sm-app` in the default installation directory.

```bash
cs install esw-sm-app:<version | SHA>
```

One can specify installation directory like the following:

```bash
cs install \
    --install-dir /tmt/apps \
    esw-sm-app:<version | SHA>
```
@@@note
If you don't provide the version or SHA in above command, `esw-sm-app` will be installed with the latest tagged binary of `esw-sm-app`
@@@

## 4. Run sequence manager via install esw-sm-app

Once esw-sm-app is installed, one can simply run esw-sm-app by executing start command

Start command supports following arguments:

- `-o` : Config file path which has mapping of sequencers and resources needed for different observing modes
- `-l` : optional argument (true if config is to be read locally or false if from remote server) default value is false
- `-a` : optional argument: agentPrefix on which Sequence Manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc.
          This argument is used when Sequence Manager is spawned using Agent. For starting standalone Sequence Manager for testing or on local
          this argument is not needed.

This command starts Sequence Manager as well as its HTTP server.

```bash
//cd to installation directory
cd /tmt/apps

// run Sequence Manager
./esw-sm-app start -o obsmode.conf
```

@@@note
Refer to supported arguments section or `./esw-sm-app start --help` for starting Sequence Manager with specific arguments
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

// run Sequence Manager
./esw-sm-app -J-Dcsw-logging.component-log-levels.ESW.sequence_manager=TRACE start -o obsmode.conf
```

# Starting Sequence Manager in simulation mode

## Prerequisites for Running esw-sm-app App in simulation mode

## 1. Install `coursier` and the TMT Apps channel

The `coursier` application must be installed on your machine, and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Start any Needed CSW services

* To run esw-sm-app in simulation mode, the **CSW Location Service** must be running.

## 3. Install esw-services

The following command creates an executable file named `esw-services` in the default installation directory.

```bash
cs install esw-services:<version | SHA>
```

## 4. Run esw-sm-app in simulation mode

```bash
cs launch esw-services – start -s --simulation
//  where:
//    -s : to start SequenceManager
//    --simulation : in simulation mode
```
    
## 5. Running esw-sm-app in simulation mode independent of esw-services

```bash
cs launch esw-sm-app:<version | SHA> -- start --simulation
```   

@@@ warning
If esw-sm-app is started independent of esw-services in the simulation mode then following things are needed to be taken care of:
* Agents won't be spawned automatically, they have to be started manually.
* A version.conf needs to be created in config-service for the Provision api to work. 
@@@


