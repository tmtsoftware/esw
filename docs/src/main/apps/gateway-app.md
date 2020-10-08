# ESW Gateway App

A command line application that facilitates starting ESW Gateway Server

## Prerequisite

- Location server should be running.
- CSW AAS should be running.

## How to start ESW Gateway Server

#### Running esw gateway server using Coursier

* Add TMT Apps channel to your local Coursier installation using below command

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

* After adding TMT apps channel you can simply launch gateway-server by executing start command

Start command supports following arguments:

 * `--port` , `-p` : Optional argument: HTTP server will be bound to this port. If a value is not provided, port will be picked up from configuration
 * `-l`, `--local` : optional aregument (true if config is to be read locally or false if from remote server) default value is false
 * `-c`, `--commandRoleConfigPath` : specifies command role mapping file path which gets fetched from config service or local file system based on --local option
 * `-m`, `--metrics` : optional argument: If true, enable gateway metrics. If not provided, default value is false and metrics will be disabled


@@@notes
On starting gateway app, it will be registered in location service as `ESW.EswGateway` as HttpRegistration. This prefix is picked up
from application.conf file
@@@

### Examples:

```bash
cs launch gateway-server:<version | SHA> -- start -p 8090 -l -c command-role-mapping.conf
```

```bash
cs launch gateway-server:<version | SHA> -- start -c command-role-mapping.conf -m
```

Note: If you don't provide the version or SHA in above command, `gateway-serve` will start with the latest tagged binary of `esw-gateway-server`

### Setting the default log level

The default log level for any component is specified in the `application.conf` file of the component.
Use the java -D option to override configuration values at runtime.  For log level, the format is:

```
-Dcsw-logging.component-log-levels.<Subsystem>.<ComponentName>=<LEVEL>
```

For example, using the example above:

```
cs launch --java-opt -Dcsw-logging.component-log-levels.ESW.EswGateway=TRACE gateway-server:<version | SHA> -- start -c command-role-mapping.conf -m
```
