#The Agent Service Application

This application will start the AgentService's server.

@@@ warning
To access any protected `AgentService` endpoints, `ESW-user` role is required.
@@@


## Running Agent Service App using Coursier

The following steps should be followed to start Agent Service Application

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine, and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).

## 2. Start Any Needed CSW Services

* To run agent service app, the **CSW Location Service** must be running.
* CSW AAS should be running.

Information on starting CSW services is @extref[here](csw:commons/apps)

## 3. Install esw-agent-service-app

Following command creates an executable file named esw-agent-service-app in the default installation directory.

```bash
cs install esw-agent-service-app
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    esw-agent-service-app
```
Note: If you don't provide the version or SHA in above command, `esw-agent-service-app` will be installed with the latest tagged binary of `esw-agent-service-app`

## 4. Run esw-agent-service-app

Once esw-agent-service-app is installed, one can simply run esw-agent-service-app by executing start command

Start command supports following arguments:

- `-p` : optional argument: port on which HTTP server will be bound. If a value is not provided, it will be randomly picked.

```bash
//cd to installation directory
cd /tmt/apps

// run agent service app
./esw-agent-service-app start
```

### Setting the Default Log Level

The default log level for any component is specified in the `application.conf` file of the component.
Use the Java -J-D option to override configuration values at runtime.  For log level, the format is:

```
-J-Dcsw-logging.component-log-levels.<Subsystem>.<ComponentName>=<LEVEL>
```

For example, using the example above:

```bash
//cd to installation directory
cd /tmt/apps

// run Sequence Manager
./esw-agent-service-app -J-Dcsw-logging.component-log-levels.ESW.agent_service=TRACE start
```
