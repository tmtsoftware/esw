#Agent Service App

This application will start the AgentService's server.

##Protection on the Agent Service endpoints
To access any protected `AgentService` endpoints, `ESW-user` role is required.

## Prerequisite

 - Location server should be running.

## How to start Agent Service

#### Running agent-service-app using Coursier

- Add TMT apps channel to your local Coursier installation using below command

```bash
cs install --add-channel https://raw.githubusercontent.com/tmtsoftware/osw-apps/master/apps.json
```

- After adding TMT apps channel you can simply launch agent-service-app by executing

```bash
cs launch agent-service-app:<version | SHA> -- start
```

Note: If you don't provide the version or SHA in above command, `agent-service-app` will start with the latest tagged binary of `esw-agent-service`
