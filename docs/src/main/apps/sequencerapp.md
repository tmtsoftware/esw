# sequencer-app

A command line application that facilitates starting Sequence Component and/or Sequencer.

## Supported Commands

* seqcomp
* sequencer 

### Sequence Component (seqcomp)

Spawns a new Sequence Component with provided `subsytem` and `name`. 
Note that with this command, only sequence component is spawned, not a sequencer.
A separate `loadScript` command needs to be sent to the sequence component to spawn a sequencer inside it.

See @ref:[sequencer](#sequencer-sequencer-) command to spawn a sequence component, and a sequencer in single command.

Options accepted by this command are described below:

 * `-s` : subsystem of the sequence component, for e.g. `tcs`, `iris` etc
 * `-n`, `--name` : optional name for sequence component, for e.g. `primary`, `backup` etc
 
#### Examples:

```
esw-ocs-app seqcomp -s tcs -n primary
```

```
esw-ocs-app seqcomp -s tcs
```

@@@note
If sequence component name is not specified, a new name (prefixed with `subsystem`) will be generated for the sequence component. 
For e.g. `TCS_123`, `IRIS_123` 
@@@

### Sequencer (sequencer)

Spawns two things:

* **SequenceComponent:** with provided `subsystem`, `name`
* **Sequencer:** with provided `observing mode` and 
`subsytem` of sequencer (`-i` option) if specified or else `subsystem` of sequence component (`-s` option) 


Options accepted by this command are described below:

 * `-s` : subsystem of the sequence component, for e.g. `tcs`, `iris` etc
 * `-n`, `--name` : optional name for sequence component, for e.g. `primary`, `backup` etc
 * `-i` : optional subsystem of sequencer script, for e.g. `tcs`, `iris` etc. Default value: subsystem provided by `-s` option
 * `-m`, `--mode` : observing mode, for e.g. `darknight`
 
#### Examples:

Below example will spawn a sequence component `OCS-primary` and a sequencer `TCS-darknight` in it.
```
esw-ocs-app sequencer -s ocs -n primary -i tcs -m darknight
```

Example below will spawn a sequence component `IRIS-primary` and a sequencer `IRIS-darknight` in it.
```
esw-ocs-app sequencer -s iris -n primary -m darknight
```

### Setting the default log level

The default log level for any component is specified in the `application.conf` file of the component.  In this case,
the Sequence Component is shared code among all Sequencers.  Therefore, to specify a log level for your Sequencer, 
use the java -D option to override configuration values at runtime.  For log level, the format is:

```
-Dcsw-logging.component-log-levels.<Subsystem>.<observingMode>=<LEVEL>
```

For example, using the example above:

```
esw-ocs-app sequencer -s iris -n primary -m darknight -Dcsw-logging.component-log-levels.IRIS.darknight=TRACE
```
