# Gateway Metrics Dev Setup

## Starting Gateway by enabling metrics

- Make sure you have all the CSW services up and running
- Run `scripts/csw-services.sh start` command after cloning `csw` repo
- Start sample HCD to test command service metrics by running `sbt "esw-integration-test/test:runMain esw.demo.Main"`
- Run `sbt "esw-gateway-server/run start -m -c scripts/metrics/commandRoles.conf"` (-m: enables metrics) (-c: enables auth roles)
    - This will start gateway on port `8090`
    - You can see a current snapshot of metrics at `http://localhost:8090/metrics`

## Visualizing Gateway Metrics using Prometheus and Grafana
- Go to `scripts/metrics` directory
- Run `docker-compose up` command

This will start following two services:
1. Prometheus Server: 
    - It is started on port `9090`, you can browse it at url: `http://localhost:9090/`
    - By default, Prometheus is configured to scrape metrics from gateway endpoint: `http://localhost:8090/metrics`
    - You can go to targets and see if Gateways health is `Up`
    
2. Grafana:
    - It is started on port `3000`
    - Default `username:password` is `admin:admin`
    - By default, it loads Gateway dashboard
    - You can go to manage, then dashboard and then select Gateway
    - This will display all the metrics in graphical view
    
## Generating Test Data
- Run `scripts/metrics/getEvent.sh` script to send `GetEvent` http request/sec to Gateway
- Run `scripts/metrics/publish.sh` script to send 5 `PublishEvent` http request/sec to Gateway
- Run `scripts/metrics/submit.sh` script to send `Submit` Component Command http request/sec to sample HCD via Gateway
- Run `scripts/metrics/validate.sh` script to send 5 `Validate` Component Command http request/sec to sample HCD via Gateway

    