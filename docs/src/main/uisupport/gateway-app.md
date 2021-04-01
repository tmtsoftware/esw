# Running the UI Gateway App

`gateway-server` is a command line application that starts the ESW UI Gateway Server. The UI Gateway Server
allows browser-based user interface programs using to interact securely with the CSW-based components.

## Prerequisites for Running gateway-server

The following steps should be followed to use gateway-server to start the UI Gateway Server.

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine and the OCS Apps channel must also be installed.
The instructions for doing this are provided @ref:[here](../technical/apps/getting-apps.md).

## 2. Start Any Needed CSW Services

* To run gateway-server, the **CSW Location Service** must be running.
* Gateway-server requires CSW AAS to be running.
* Any other CSW Services needed by scripts or browser UIs should also be running.

Information on starting CSW services is @extref[here](csw:commons/apps)

## 3. Install gateway-server

The following command creates an executable file named gateway-server in the default installation directory.

```bash
cs install gateway-server:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    gateway-server:<version | SHA>
```

@@@ note
If you don't provide the version or SHA in the above command, `gateway-server` will be installed with the latest tagged binary of `esw-gateway-server`.

@@@

### 4. Run gateway-server

Once gateway-server is installed, one can simply run gateway-server by executing the `start` command.

Start command supports following arguments:

 * `--port` , `-p` : Optional argument: HTTP server will be bound to this port. If a value is not provided, port will be picked up from a default configuration.
 * `-l`, `--local` : optional argument (true if config is to be read locally or false if from CSW Configuration Service) default value is false.
 * `-c`, `--commandRoleConfigPath` : specifies command role mapping file path which gets fetched from CSW Configuration Service or local file system based on --local option.
 * `-m`, `--metrics` : optional argument: If true, enable gateway metrics. If not provided, default value is false and metrics will be disabled.

@@@note

On starting gateway server app, it will be registered in location service as `ESW.EswGateway` as HttpRegistration. This prefix is picked up
from application.conf file.

@@@

The following example commands start the Gateway Server.

Example 1: Local command map with metrics enabled
```bash
//cd to installation directory
cd /tmt/apps

// run gateway server with provided port, local command role config file and with metrics enabled
./gateway-server start -p 8090 -l -c command-role-mapping.conf -m
```

Example 2: Use the Configuration Service to provide the command role mapping
```bash
//cd to installation directory
cd /tmt/apps

// run gateway server with remote command role config file
./gateway-server start -c command-role-mapping.conf
```

@@@note

Refer supported arguments section or `./gateway-server start --help` for starting gateway server with specific arguments.

@@@

## Setting the Default Log Level

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
./gateway-server -J-Dcsw-logging.component-log-levels.ESW.EswGateway=TRACE start -p 8090 -l -c command-role-mapping.conf
```
