#Agent App

This application will start the Agent actor.

## Prerequisite

 - Location server should be running.

## How to start Agent

#### Running agent-app using Coursier

- Add TMT apps channel to your local Coursier installation using below command

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.prod.json
```

- After adding TMT apps channel you can simply launch agent-app by executing

```bash
cs launch agent-app:<version | SHA> -- start
```

Note: If you don't provide the version or SHA in above command, `agent-app` will start with the latest tagged binary of `esw-agent-akka-app`
