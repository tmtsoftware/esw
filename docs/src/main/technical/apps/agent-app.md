# The Agent Application

This application will start the Agent Actor. The Agent application can launch or spawn applications including: Sequence Manager,
Sequence Component, and CSW containers. Once the Agent Application is started on a machine, it registers itself with CSW
Location Service and is available for spawning applications through its remote API, which is used primarily by the
Agent Service documented @ref[here](agent-service-app.md).

## Running Agent App using Coursier

The following steps should be followed to start Agent

## 1. Install `coursier` and the TMT Apps Channel

The `coursier` application must be installed on your machine, and the OCS Apps channel must be installed.
The instructions for doing this are provided @ref:[here](getting-apps.md).


## 2. Start Any Needed CSW Services

* To run agent, the **CSW Location Service** must be running.

Information on starting CSW services is @extref[here](csw:commons/apps)

## 3. Install esw-agent-akka-app

Following command creates an executable file named esw-agent-akka-app in the default installation directory.

```bash
cs install esw-agent-akka-app
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    esw-agent-akka-app
```
Note: If you don't provide the version or SHA in above command, `esw-agent-akka-app` will be installed with the latest tagged binary of `esw-agent-akka-app`

## 4. Run esw-agent-akka-app

Once esw-agent-akka-app is installed, one can simply run esw-agent-akka-app by executing start command

Start command supports following arguments:

- `-p`: prefix of machine. For example, tcs.primary_machine, ocs.machine1 etc.

```bash
//cd to installation directory
cd /tmt/apps

// run agent app
./esw-agent-akka-app start -p "tcs.primary_machine"
```

### Setting the Default Log Level

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
./esw-agent-akka-app -J-Dcsw-logging.component-log-levels.TCS.primary_machine=TRACE start -p "tcs.primary_machine"
```
