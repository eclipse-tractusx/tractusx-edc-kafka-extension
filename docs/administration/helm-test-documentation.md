# Helm Test Documentation - EDC Kafka Demo

## Introduction

This document provides comprehensive information about the Helm tests for the EDC Kafka Demo chart. These tests use the consumer application to validate the correct functionality of the kafka-broker-extension, ensuring it properly handles Kafka asset subscriptions and data consumption.

## Test Overview

The test suite consists of three automated tests that verify the consumer application's ability to:
1. Successfully subscribe to and consume data from Kafka assets
2. Handle different asset types (forecast and tracking data)
3. Properly handle error scenarios (non-existing assets)

All tests are executed automatically after deploying the Helm chart using the `helm test` command.

## Test Cases

### Test Case 1: Consumer Subscribe - Forecast Asset

**Test Name:** `test-consumer-subscribe`

**Hook Weight:** 5 (executes first)

**Objective:** Verify that the consumer application can successfully subscribe to the `kafka-forecast-asset` and receive data.

**Test Endpoint:** `POST /api/subscribe?assetId=kafka-forecast-asset`

**Test Steps:**
1. Wait for the consumer-app service to be available on port 8080
2. Send POST request to the subscribe endpoint with `assetId=kafka-forecast-asset`
3. Extract HTTP status code and response body
4. Validate HTTP status code is in the 2xx range (success)
5. Verify response contains the `recordCount` field
6. Extract and validate that `recordCount` is greater than 0

**Expected Results:**
- HTTP Status Code: 200-299 (success)
- Response Body: JSON containing `recordCount` field
- Record Count: Greater than 0

**Success Criteria:**
```
SUCCESS: Subscribe endpoint test passed with recordCount={value}
```

**Failure Scenarios:**
- HTTP status code is not in 2xx range
- Response does not contain `recordCount` field
- Record count is 0 or negative

---

### Test Case 2: Consumer Subscribe - Tracking Asset

**Test Name:** `test-consumer-subscribe-tracking`

**Hook Weight:** 6 (executes second)

**Objective:** Verify that the consumer application can successfully subscribe to the `kafka-tracking-asset` and receive data.

**Test Endpoint:** `POST /api/subscribe?assetId=kafka-tracking-asset`

**Test Steps:**
1. Wait for the consumer-app service to be available on port 8080
2. Send POST request to the subscribe endpoint with `assetId=kafka-tracking-asset`
3. Extract HTTP status code and response body
4. Validate HTTP status code is in the 2xx range (success)
5. Verify response contains the `recordCount` field
6. Extract and validate that `recordCount` is greater than 0

**Expected Results:**
- HTTP Status Code: 200-299 (success)
- Response Body: JSON containing `recordCount` field
- Record Count: Greater than 0

**Success Criteria:**
```
SUCCESS: Subscribe endpoint test passed with recordCount={value}
```

**Failure Scenarios:**
- HTTP status code is not in 2xx range
- Response does not contain `recordCount` field
- Record count is 0 or negative

---

### Test Case 3: Consumer Subscribe - Error Handling

**Test Name:** `test-consumer-subscribe-error`

**Hook Weight:** 7 (executes third)

**Objective:** Verify that the consumer application properly handles subscription requests for non-existing assets and returns appropriate error responses.

**Test Endpoint:** `POST /api/subscribe?assetId=not-existing`

**Test Steps:**
1. Wait for the consumer-app service to be available on port 8080
2. Send POST request to the subscribe endpoint with `assetId=not-existing` (non-existing asset)
3. Extract HTTP status code and response body
4. Validate HTTP status code is 502 (BAD_GATEWAY)
5. Verify response body contains the error message "EDC request failed"

**Expected Results:**
- HTTP Status Code: 502 (BAD_GATEWAY)
- Response Body: Contains error message "EDC request failed"

**Success Criteria:**
```
SUCCESS: Error handling test passed - received BAD_GATEWAY with correct error message
```

**Failure Scenarios:**
- HTTP status code is not 502
- Response does not contain "EDC request failed" error message

---

## Test Configuration

### Test Pod Specifications

All test pods share the following configuration:

**Image:** `curlimages/curl:8.4.0`

**Security Context:**
- `runAsNonRoot: true`
- `runAsUser: 65534` (nobody user)
- `readOnlyRootFilesystem: true`
- `allowPrivilegeEscalation: false`
- Capabilities: All dropped

**Restart Policy:** Never

**Helm Annotations:**
- `helm.sh/hook: test` - Marks the pod as a Helm test
- `helm.sh/hook-weight: {5,6,7}` - Defines execution order
- `helm.sh/hook-delete-policy: before-hook-creation,hook-succeeded` - Cleanup policy

### Test Execution Order

Tests are executed sequentially based on their hook weight:
1. **Weight 5**: Test forecast asset subscription (test-consumer-subscribe)
2. **Weight 6**: Test tracking asset subscription (test-consumer-subscribe-tracking)
3. **Weight 7**: Test error handling (test-consumer-subscribe-error)

## How to Run the Tests

### Prerequisites

Before running the tests, ensure:

1. **Kubernetes cluster is available** (Minikube, Kind, or cloud provider)
2. **Helm 3.x is installed**
3. **kubectl is configured** to access your cluster
4. **EDC runtimes are built**:
   ```bash
   ./poc/gradlew -p poc dockerize
   ```
5. **Helm chart dependencies are updated**:
   ```bash
   helm dependency update charts/tractusx-edc-kafka
   helm dependency update charts/edc-kafka-demo
   ```

### Installation and Testing

1. **Install the Helm chart**:
   ```bash
   helm install demo charts/edc-kafka-demo -n demo --create-namespace
   ```

2. **Wait for all pods to be ready** (approximately 3 minutes):
   ```bash
   kubectl get pods -n demo -w
   ```

3. **Run the Helm tests**:
   ```bash
   helm test demo -n demo
   ```

### Expected Output

When all tests pass successfully, you should see output similar to:

```
NAME: demo
LAST DEPLOYED: [timestamp]
NAMESPACE: demo
STATUS: deployed
REVISION: 1
TEST SUITE:     demo-test-consumer-subscribe
Last Started:   [timestamp]
Last Completed: [timestamp]
Phase:          Succeeded

TEST SUITE:     demo-test-consumer-subscribe-tracking
Last Started:   [timestamp]
Last Completed: [timestamp]
Phase:          Succeeded

TEST SUITE:     demo-test-consumer-subscribe-error
Last Started:   [timestamp]
Last Completed: [timestamp]
Phase:          Succeeded
```

### Viewing Test Logs

To view detailed logs for each test:

```bash
# View logs for forecast asset test
kubectl logs demo-test-consumer-subscribe -n demo

# View logs for tracking asset test
kubectl logs demo-test-consumer-subscribe-tracking -n demo

# View logs for error handling test
kubectl logs demo-test-consumer-subscribe-error -n demo
```

### Re-running Tests

To re-run the tests after making changes:

```bash
# Delete existing test pods (if any)
kubectl delete pod -l 'helm.sh/chart=edc-kafka-demo' -n demo

# Run tests again
helm test demo -n demo
```

## Troubleshooting

### Test Failures

If a test fails, follow these steps:

1. **Check test pod logs**:
   ```bash
   kubectl logs <test-pod-name> -n demo
   ```

2. **Verify consumer-app is running**:
   ```bash
   kubectl get pods -n demo | grep consumer-app
   kubectl logs deployment/demo-consumer-app -n demo
   ```

3. **Check service connectivity**:
   ```bash
   kubectl get svc -n demo | grep consumer-app
   ```

4. **Verify EDC components are healthy**:
   ```bash
   kubectl get pods -n demo
   ```

### Common Issues

**Issue 1: Consumer-app service not available**
- **Symptom**: Test keeps waiting for consumer-app:8080
- **Solution**: Check if consumer-app pod is running and healthy
  ```bash
  kubectl describe pod -l app=consumer-app -n demo
  ```

**Issue 2: HTTP 502 for valid assets**
- **Symptom**: Test cases 1 or 2 receive 502 instead of 2xx
- **Solution**: Check EDC connector logs and verify asset configuration
  ```bash
  kubectl logs deployment/demo-edc-provider -n demo
  ```

**Issue 3: HTTP 200 for non-existing asset**
- **Symptom**: Test case 3 receives 200 instead of 502
- **Solution**: Verify error handling in consumer-app and EDC configuration

**Issue 4: recordCount is 0**
- **Symptom**: Valid subscription but no records received
- **Solution**: Check if producer is running and generating data
  ```bash
  kubectl logs deployment/demo-producer-app -n demo
  ```

## Test Results Summary

| Test Case                        | Asset ID             | Expected Status | Expected Behavior                           | Status |
|----------------------------------|----------------------|-----------------|---------------------------------------------|--------|
| test-consumer-subscribe          | kafka-forecast-asset | 200-299         | recordCount > 0                             | ✓ Pass |
| test-consumer-subscribe-tracking | kafka-tracking-asset | 200-299         | recordCount > 0                             | ✓ Pass |
| test-consumer-subscribe-error    | not-existing         | 502             | Error message contains "EDC request failed" | ✓ Pass |

Latest Test results can be viewed in this GitHub workflow: [Deployment Tests](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension/actions/workflows/deployment-test.yaml)

## Cleanup

After testing, you can clean up the test pods and uninstall the chart:

```bash
# Test pods are automatically cleaned up based on hook-delete-policy

# Uninstall the entire chart
helm uninstall demo -n demo

# Delete the namespace (optional)
kubectl delete namespace demo
```

## Additional Resources

- [Admin Manual](admin-manual.md) - Detailed administration guide
- [Kafka Consumer/Producer Apps Documentation](kafka-consumer-producer-apps.md) - Application-specific documentation
- [Helm Chart README](../../charts/edc-kafka-demo/README.md) - Chart configuration and values
- [EDC Documentation](https://eclipse-edc.github.io/docs/) - Eclipse Dataspace Connector documentation
