# Starting the Sequence Manager Application

`esw-sm-app` is a command line application that facilitates starting Sequence Manager, and the HTTP server that is part of
Sequence Manager using the `coursier` dependency management application. The `coursier` tool is described with full documentation
at the [coursier site](https://get-coursier.io).

The following steps should be followed to use esw-sm-app to start a Sequencer Manager.

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Start needed CSW services

To run esw-sm-app:

* the CSW Location Service must be running,
* And CSW Authorization and Authentication Service should be running.

Information on starting CSW services is @extref[here](csw:commons/apps).

## 3. Install esw-sm-app

The following `coursier` command creates an executable file named `esw-sm-app` in the default coursier installation directory.

```bash
cs install esw-sm-app
```

One can specify the installation directory like the following:

```bash
cs install --install-dir /tmt/apps esw-sm-app
```

@@@note
If you don't provide the version *or* SHA in above command, `esw-sm-app` will be installed with the latest tagged binary of `esw-sm-app`.
@@@

## 4. Run Sequence Manager with the installed esw-sm-app

Once esw-sm-app is installed, one can simply execute esw-sm-app with the start command.

Start command supports following arguments:

* `-p` : optional argument: port on which HTTP server will be bound. If a value is not provided, it will be randomly picked.
* `-o` : Config file path which has mapping of sequencers and resources needed for different observing modes
* `-l` : optional argument (true if config is to be read locally or false if from the Configuration Service) default value is false
* `-a` : optional argument: This argument is used when Sequence Manager is spawned using Agent. It is the prefix for the Agent on
        which the Sequence Manager will be spawned (examples: ESW.agent1, IRIS.agent2 etc). For starting standalone Sequence Manager for testing or on local
        this argument is not needed.

This command starts Sequence Manager as well as its HTTP interface.

```bash
//cd to installation directory
cd /tmt/apps

// run Sequence Manager
./esw-sm-app start -o obsmode.conf
```

@@@note
Refer to supported arguments section above or `./esw-sm-app start --help` for starting Sequence Manager with specific arguments.
@@@

### Sequence Manager ObsMode Configuration File

The Sequence Manager's job is to start Sequencers, manage observatory resources and determine when obsModes can execute concurrently.
The Sequence Manager is configured with a file that specifies available Observing Modes or "obsModes".  Each obsMode
has a list of resources required by the obsMode and a list of Sequencers that are started for the obsMode.

Resources are currently specified using subsystem names such as ESW or TCS. Sequencers are also specified by a subsystem name. In this case,
Sequence Manager uses the obsMode and the subsystem to find the correct script to load in the subsystem's Sequencer. The scripts
are located in the `sequencer-scripts` repository.

The obsMode configuration file is written in JSON. An example follows with three obsModes: IRIS_MCAO, IRIS_Calib, and WFOS_Calib.
By convention obsModes start with the instrument's subsystem name followed by an underscore and some other description. The Sequencers can also have a variation now(eg. `IRIS.red`, `IRIS.blue`) to run multiple sequencers concurrently for a subsystem.
```
esw-sm {
  obsModes: {
    IRIS_Darknight: {
      resources: [IRIS, TCS, NFIRAOS]
      sequencers: [IRIS, ESW, TCS]
    },
    IRIS_ImagerAndIFS: {
      resources: [IRIS, TCS]
      sequencers: [IRIS.red, IRIS.blue, ESW]
    },
    WFOS_Calib: {
      resources: [WFOS]
      sequencers: [ESW, WFOS]
    },
    IRIS_FilterWheel: {
      resources: [IRIS, TCS, NFIRAOS]
      sequencers: [IRIS, ESW, TCS, AOESW]
    }
  }
}
```

### Setting the Log Level

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

## Starting Sequence Manager in Simulation Mode

The Sequence Manager supports a simulation mode, which is primarily useful for standalone testing of the Sequence
Manager. When started in simulation mode, the Sequence Manager starts bypasses the Agents, and starts Sequence Components
internally. The register themselves with the Location Service and load Scripts and Sequencers as normal, but simulated
Sequence Manager can start on its own.

### 1. Install `coursier` and add the TMT Apps channel

The `coursier` application must be installed on your machine, and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

### 2. Start any Needed CSW services

* To run esw-sm-app in simulation mode, the **CSW Location Service** must be running.

### 3. Install esw-services

The following command creates an executable file named `esw-services` in the default installation directory.

```bash
cs install esw-services
```

## 4. Run esw-sm-app in Simulation Mode

```bash
cs launch esw-services -- start -s --simulation
//  where:
//    -s : to start SequenceManager
//    --simulation : in simulation mode
```

## 5. Running esw-sm-app in Simulation Mode Without esw-services

```bash
cs launch esw-sm-app -- start --simulation
```

@@@ warning
If esw-sm-app is started independent of esw-services in the simulation mode then following things are needed to be taken care of:

* Agents won't be spawned automatically, they have to be started manually.
* A version.conf needs to be created in config-service for the Provision api to work.
@@@
