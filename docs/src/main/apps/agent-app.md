#Agent App

This application will start the Agent actor.

## Prerequisite

 - Location server should be running.

## How to start Agent

### Running Agent App using Coursier

* Add TMT Apps channel to your local Coursier installation using below command

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

* Install agent-app

Following command creates an executable file named agent-app in the default installation directory.

```bash
cs install agent-app:<version | SHA>
```

One can specify installation directory like following:

```bash
cs install \
    --install-dir /tmt/apps \
    agent-app:<version | SHA>
```
Note: If you don't provide the version or SHA in above command, `agent-app` will be installed with the latest tagged binary of `esw-agent-akka-app`

* Run agent-app

Once agent-app is installed, one can simply run agent-app by executing start command

Start command supports following arguments:

- `-p` : prefix of machine. tcs.primary_machine, ocs.machine1 etc

```bash
//cd to installation directory
cd /tmt/apps

// run agent app
./agent-app start -p "tcs.primary_machine"
```

### Setting the default log level

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
./agent-app -J-Dcsw-logging.component-log-levels.TCS.primary_machine=TRACE start -p "tcs.primary_machine"
```
