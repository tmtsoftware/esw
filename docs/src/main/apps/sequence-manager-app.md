# Sequence Manager App

A command line application that facilitates starting Sequence Manager and HTTP server of Sequence Manager.

## Prerequisite

- Location server should be running.
- CSW AAS should be running.

## How to start Sequence Manager App

### Running sequence manager using Coursier

* Add TMT Apps channel to your local Coursier installation using below command

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

* Install sequence-manager app

Following command creates an executable file named sequence-manager in the default installation directory.

```bash
cs install sequence-manager:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    sequence-manager:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `sequence-manager` will be installed with the latest tagged binary of `esw-sm-app`

* Run sequence manager app

Once sequence-manager is installed, one can simply run sequence-manager by executing start command

Start command supports following arguments:

- `-o` : Config file path which has mapping of sequencers and resources needed for different observing modes
- `-l` : optional aregument (true if config is to be read locally or false if from remote server) default value is false
- `-a` : optional argument: agentPrefix on which sequence manager will be spawned, ex: ESW.agent1, IRIS.agent2 etc.
          This argument is used when Sequence Manager is spawned using Agent. For starting standalone sequence manager for testing or on local
          this argument is not needed.


This command starts Sequence Manager as well as HTTP server of Sequence Manager.

```bash
//cd to installation directory
cd /tmt/apps

// run sequence manager
./sequence-manager start -o obsmode.conf
```

### Setting the log level

The default log level for any component is specified in the `application.conf` file of the component.
Use the java -D option to override configuration values at runtime.  For log level, the format is:

```
-Dcsw-logging.component-log-levels.<Subsystem>.<ComponentName>=<LEVEL>
```

For example, using the example above:

```bash
//cd to installation directory
cd /tmt/apps

// run sequence manager
./sequence-manager -J-Dcsw-logging.component-log-levels.ESW.sequence_manager=TRACE start -o obsmode.conf
```
