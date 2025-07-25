# edc-kafka-demo

## Install

Build the edc runtimes:

```bash
docker build poc/runtimes/edc/dataplane-local -t tractusx/edc-dataplane-kafka:test
docker build poc/runtimes/edc/controlplane-local -t tractusx/edc-controlplane-kafka:test
```

Update the chart dependencies:

```bash
helm dependency update charts/tractusx-edc-kafka
helm dependency update charts/edc-kafka-demo
```

Install the edc-kafka-demo helm chart:

```bash
helm install demo charts/edc-kafka-demo -n demo --create-namespace
```

### Verify the installation

It takes approx. 3 Minutes (may vary depending on your machine's resources) for all components to be ready. 
Once all pods are started, inspect the consumer-app logs to see the dataflow in action: 

```bash
kubectl logs deployment/demo-consumer-app -n demo --max-log-requests=1 -f
```
