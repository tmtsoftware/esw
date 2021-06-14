# Running a Sequencer Using ocs-app

The `ocs-app` is a command line application that facilitates starting a Sequence Component and/or Sequencer 
using the `coursier` dependency management application. The `coursier` tool is described with full documentation 
at the [coursier site](https://get-coursier.io).

## Prerequisites for Running ocs-app

The following steps should be followed to use ocs-app to start a Sequencer or Sequence Component.

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine, and the OCS Apps channel must be installed.
The instructions for doing this is provided @ref:[here](../technical/apps/getting-apps.md).

## 2. Install ocs-app

The following command creates an executable file named `ocs-app` in the default installation directory.

```bash
cs install ocs-app:<version | SHA>
```

One can specify an installation directory like the following:

```bash
cs install \
    --install-dir /tmt/apps \
    ocs-app:<version | SHA>
```

@@@note
If you don't provide the version or SHA in the above command, `ocs-app` will be installed with the latest tagged binary of `esw-ocs-app`

@@@

## 3. Start Any Needed CSW Services

* To run Sequencers and Sequence Components, the **CSW Location Service** must be running.
* Any other CSW Services needed by scripts should also be running.

Information on starting CSW services is @extref[here](csw:commons/apps)

## 4. Run ocs-app

Supported Commands

* seqcomp - starts sequence component
* sequencer - starts sequence components and sequencer in single command

### Starting a Sequence Component

Ocs-app spawns a new Sequence Component with a provided `subsytem` and `name`.
Note that with this command, only a sequence component is spawned, not a sequencer.
A separate `loadScript` command needs to be sent to the sequence component to spawn a sequencer inside it.

Options accepted by this command are described below:

 * `-s` : subsystem of the sequence component, for e.g. `tcs`, `iris` etc
 * `-n`, `--name` : optional name for sequence component, for e.g. `primary`, `backup` etc

Once ocs-app is installed, one can simply run sequencer or sequence component by executing the `start` command.

Start command supports following arguments:

 * `--port`, `-p` : Optional argument: HTTP server will be bound to this port. If a value is not provided, port will be picked up from configuration.
 * `-l`, `--local` : optional argument (true if config is to be read locally or false if from remote server) default value is false.
 * `-c`, `--commandRoleConfigPath` : specifies command role mapping file path which gets fetched from config service or local file system based on --local option.
 * `-m`, `--metrics` : optional argument: If true, enable gateway metrics. If not provided, default value is false and metrics will be disabled.


Here are some examples. 

Example 1: Starts a Sequence Component with a name
```bash
//cd to installation directory
cd /tmt/apps

//start sequence component with specified name
./ocs-app seqcomp -s tcs -n primary
```

Example 2: Starts a Sequence Component without a name 
```bash
//cd to installation directory
cd /tmt/apps

//start sequence component without name argument.
./ocs-app seqcomp -s tcs
```

@@@note
If the Sequence Component name is not specified, a new name (prefixed with `subsystem`) will be generated for the Sequence Component.
For e.g. `TCS_123`, `IRIS_123`
Refer to supported arguments section or `./ocs-app start --help` for starting ocs-app with specific arguments
@@@

### Starting a Sequencer

When starting a Sequencer, ocs-app spawns two things:

* **Sequence Component:** with provided `subsystem`, `name`
* **Sequencer:** with provided `observing mode` and
`subsystem` of sequencer (`-i` option) if specified or else `subsystem` of sequence component (`-s` option)

Options accepted by this command are described below:

 * `-s` : subsystem of the sequence component, for e.g. `tcs`, `iris` etc.
 * `-n`, `--name` : optional name for sequence component, for e.g. `primary`, `backup` etc.
 * `-i` : optional subsystem of sequencer script, for e.g. `tcs`, `iris` etc. Default value: subsystem provided by `-s` option.
 * `-m`, `--mode` : observing mode, for e.g. `darknight`.

The following command examples start both a Sequence Component and Sequencer:

Example 1: Start a Sequencer with TCS darknight observing mode
```bash
//cd to installation directory
cd /tmt/apps

//Below example will spawn a Sequence Component called `OCS.primary` and a Sequencer `TCS.darknight` in it.
./ocs-app sequencer -s ocs -n primary -i tcs -m darknight
```

Example 2: Start the IRIS-darknight Sequencer on an IRIS Sequence Component
```bash
//cd to installation directory
cd /tmt/apps

//Example below will spawn a Sequence Component `IRIS-primary` and a Sequencer `IRIS-darknight` in it.
./ocs-app sequencer -s iris -n primary -m darknight

```

@@@note
Refer supported arguments section or `./ocs-app start --help` for starting ocs-app with specific arguments.
@@@

## Setting the Default Log Level
The default log level for any component is specified in the `application.conf` file of the component.  In this case,
the Sequence Component is shared code among all Sequencers.  Therefore, to specify a log level for your Sequencer,
use the java -J-D option to override configuration values at runtime.  For log level, the format is:

```
-J-Dcsw-logging.component-log-levels.<Subsystem>.<obsMode>=<LEVEL>
```

For example, using the example above:

```bash
//cd to installation directory
cd /tmt/apps

// run sequencer iris.darknight on iris.primary sequence component
./ocs-app -J-Dcsw-logging.component-log-levels.IRIS.darknight=TRACE sequencer -s iris -n primary -m darknight
```
