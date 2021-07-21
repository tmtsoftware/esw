# Gateway Metrics Dev Setup

## Starting Gateway by enabling metrics

- Make sure you have all the CSW services up and running
- Run `scripts/csw-services.sh --version $csw_version start` command after cloning `csw` repo (use the version of csw used by esw.)
- Start sample HCD to test command service metrics by running `sbt "esw-integration-test/Test/runMain esw.demo.Main"`
- Run `sbt "esw-gateway-server/run start --local -m -c <Absolute path of scripts/metrics/commandRoles.conf>"` (-m: enables metrics) (-c: enables auth roles)
  - This will start gateway on port `8090`
  - You can see a current snapshot of metrics at `http://<gateway_address>:8090/metrics`

## Visualizing Gateway Metrics using Prometheus and Grafana

- Go to `scripts/metrics` directory
- *Replace <gateway_address> from `prometheus.yml` with the ip address of the gateway*
- Run `docker-compose up` command

This will start following two services:

1. Prometheus Server:
    - It is started on port `9090`, you can browse it at url: `http://localhost:9090/`
    - By default, Prometheus is configured to scrape metrics from gateway endpoint: `http://<gateway_address>:8090/metrics`
    - You can go to targets and see if Gateways health is `Up`

2. Grafana:
    - It is started on port `3000`
    - Default `username:password` is `admin:admin`
    - By default, it loads Gateway dashboard
    - You can go to manage, then dashboard and then select Gateway
    - This will display all the metrics in graphical view

## Generating Test Data

- Run `scripts/metrics/getEvent.sh <gateway_address>` script to send `GetEvent` http request/sec to Gateway
- Run `scripts/metrics/publish.sh <gateway_address> <access_token>` script to send 5 `PublishEvent` http request/sec to Gateway
- Run `scripts/metrics/submit.sh <gateway_address> <access_token>` script to send `Submit` Component Command http request/sec to sample HCD via Gateway
- Run `scripts/metrics/validate.sh <gateway_address> <access_token>` script to send 5 `Validate` Component Command http request/sec to sample HCD via Gateway
- Run `scripts/metrics/subscribeEvent.sh <gateway_address>` script to create new websocket connection and subscribe to event stream generated using `publish.sh` script.
- Run `scripts/metrics/subscribeEventPattern.sh <gateway_address>` script to create new websocket connection and pattern subscribe to event stream generated using `publish.sh` script.

## Notes

- Last two scripts (websocket related) requires [wscat](https://github.com/websockets/wscat) to be installed on your machine.

- <gateway_address> is your Machine's IP address.

- You can get <access_token> for osw user by running following command

```bash
curl -X POST --location "http://<host_ip_address:8081/auth/realms/TMT/protocol/openid-connect/token" -H "Content-Type: application/x-www-form-urlencoded" -d "client_id=tmt-frontend-app&grant_type=password&username=osw-user1&password=osw-user1"
```
