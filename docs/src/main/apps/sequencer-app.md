# Sequencer App

A command line application that facilitates starting Sequence Component and/or Sequencer using coursier.

## Prerequisite

- Location server should be running.
- event service should be running.

## Add TMT Apps channel to your local Coursier installation using below command

Channel needs to be added to install application using `cs install`

For developer machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

For production machine setup,

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

## Install ocs-app

Following command creates an executable file named gateway-server in the default installation directory.

```bash
cs install ocs-app:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    ocs-app:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `ocs-app` will be installed with the latest tagged binary of `esw-ocs-app`

## Run ocs-app

Supported Commands

* seqcomp - starts sequence component
* sequencer - starts sequence components and sequencer in single command

* Sequence Component (seqcomp)

Spawns a new Sequence Component with provided `subsytem` and `name`.
Note that with this command, only sequence component is spawned, not a sequencer.
A separate `loadScript` command needs to be sent to the sequence component to spawn a sequencer inside it.

Options accepted by this command are described below:

 * `-s` : subsystem of the sequence component, for e.g. `tcs`, `iris` etc
 * `-n`, `--name` : optional name for sequence component, for e.g. `primary`, `backup` etc

Once ocs-app is installed, one can simply run sequencer or sequence component by executing start command

Start command supports following arguments:

 * `--port` , `-p` : Optional argument: HTTP server will be bound to this port. If a value is not provided, port will be picked up from configuration
 * `-l`, `--local` : optional aregument (true if config is to be read locally or false if from remote server) default value is false
 * `-c`, `--commandRoleConfigPath` : specifies command role mapping file path which gets fetched from config service or local file system based on --local option
 * `-m`, `--metrics` : optional argument: If true, enable gateway metrics. If not provided, default value is false and metrics will be disabled


This command starts sequence component.

Example 1:
```bash
//cd to installation directory
cd /tmt/apps

//start sequence component with specified name
./ocs-app seqcomp -s tcs -n primary
```


Example 2:
```bash
//cd to installation directory
cd /tmt/apps

//start sequence component without name argument.
./ocs-app seqcomp -s tcs
```

@@@note
If sequence component name is not specified, a new name (prefixed with `subsystem`) will be generated for the sequence component.
For e.g. `TCS_123`, `IRIS_123`
Refer supported arguments section or `./ocs-app start --help` for starting gateway server with specific arguments
@@@

* Sequencer

Spawns two things:

* **SequenceComponent:** with provided `subsystem`, `name`
* **Sequencer:** with provided `observing mode` and
`subsytem` of sequencer (`-i` option) if specified or else `subsystem` of sequence component (`-s` option)


Options accepted by this command are described below:

 * `-s` : subsystem of the sequence component, for e.g. `tcs`, `iris` etc
 * `-n`, `--name` : optional name for sequence component, for e.g. `primary`, `backup` etc
 * `-i` : optional subsystem of sequencer script, for e.g. `tcs`, `iris` etc. Default value: subsystem provided by `-s` option
 * `-m`, `--mode` : observing mode, for e.g. `darknight`

Following command starts sequence component and sequencer both:

Example 1:
```bash
//cd to installation directory
cd /tmt/apps

//Below example will spawn a sequence component `OCS.primary` and a sequencer `TCS.darknight` in it.
./ocs-app sequencer -s ocs -n primary -i tcs -m darknight
```

Example 2:
```bash
//cd to installation directory
cd /tmt/apps

//Example below will spawn a sequence component `IRIS-primary` and a sequencer `IRIS-darknight` in it.
./ocs-app sequencer -s iris -n primary -m darknight

```

@@@notes
Refer supported arguments section or `./ocs-app start --help` for starting esw  with specific arguments
@@@

## Setting the default log level
The default log level for any component is specified in the `application.conf` file of the component.  In this case,
the Sequence Component is shared code among all Sequencers.  Therefore, to specify a log level for your Sequencer,
use the java -D option to override configuration values at runtime.  For log level, the format is:

```
-Dcsw-logging.component-log-levels.<Subsystem>.<obsMode>=<LEVEL>
```

For example, using the example above:

```bash
//cd to installation directory
cd /tmt/apps

// run sequencer iris.darknight on iris.primary sequence component
./ocs-app -J-Dcsw-logging.component-log-levels.IRIS.darknight=TRACE sequencer -s iris -n primary -m darknight
```
