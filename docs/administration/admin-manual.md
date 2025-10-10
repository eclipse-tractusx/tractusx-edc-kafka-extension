# Tractus-X Kafka Data Exchange - Admin Manual

## Introduction

This Admin Manual provides detailed instructions for setting up, configuring, and maintaining the Tractus-X Kafka Data Exchange extension. The extension enables real-time data streaming between data providers and consumers using Apache Kafka as the transport protocol within the Tractus-X ecosystem.

## System Architecture

The Kafka Data Exchange extension consists of several components that work together to enable secure data streaming:

1. **Kafka Broker Extension**: A Control Plane extension that manages access to Kafka topics by creating credentials and tokens
2. **Data Address Kafka**: Defines the data address format for Kafka assets
3. **Validator Data Address Kafka**: Validates Kafka data addresses

These components integrate with:
- **Eclipse Dataspace Connector (EDC)**: The core framework for data exchange
- **Apache Kafka**: The messaging platform for data streaming
- **OAuth Service**: For authentication and authorization (e.g., Keycloak)

### Dependencies

Add the following dependency to your EDC Control Plane:

```gradle
dependencies {
    // Kafka Broker Extension (Control Plane)
    implementation("org.eclipse.tractusx.edc:kafka-broker-extension:${version}")
}
```

### Kubernetes Deployment with Helm

For Kubernetes deployment, use the provided Helm charts. The `edc-kafka-demo` chart deploys a complete demo environment including:

- **Kafka broker** with OAuth authentication
- **Keycloak** for identity and access management
- **EDC connectors** (provider and consumer) with Kafka extension
- **Producer and Consumer applications** for testing

#### Prerequisites

- Kubernetes cluster (e.g., Minikube, Kind, or cloud provider)
- Helm 3.x installed
- kubectl configured to access your cluster

#### Installation Steps

1. **Build the EDC runtimes**:

   ```bash
   ./poc/gradlew -p poc dockerize
   ```

2. **Update chart dependencies**:

   ```bash
   helm dependency update charts/tractusx-edc-kafka
   helm dependency update charts/edc-kafka-demo
   ```

3. **Install the Helm chart**:

   ```bash
   helm install demo charts/edc-kafka-demo -n demo --create-namespace
   ```

4. **Verify the installation**:

   It takes approximately 3 minutes for all components to be ready. Once all pods are started, run the helm test:

   ```bash
   helm test demo -n demo
   ```

5. **Uninstall** (when needed):

   ```bash
   helm uninstall demo -n demo
   ```

For more details and configuration options, see the chart documentation: [charts/edc-kafka-demo](/charts/edc-kafka-demo)

## Configuration

### Kafka Configuration

Configure your Kafka broker to use OAuth authentication:

```properties
# KRaft mode (no ZooKeeper)
kafka.process.roles="broker,controller"
kafka.controller.quorum.voters="1@kafka-kraft:29093"
kafka.controller.listener.names="CONTROLLER"

# Listeners & Protocols
kafka.listener.security.protocol.map="CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,OIDC:SASL_PLAINTEXT"
kafka.listeners="PLAINTEXT://kafka-kraft:29092,CONTROLLER://kafka-kraft:29093,OIDC://0.0.0.0:9092"
kafka.advertised.listeners="PLAINTEXT://kafka-kraft:29092,OIDC://kafka-kraft:9092"
kafka.inter.broker.listener.name="PLAINTEXT"

# Enable SASL/OAUTHBEARER authentication
kafka.sasl.enabled.mechanisms="OAUTHBEARER"
kafka.sasl.oauthbearer.jwks.endpoint.url="http://keycloak:8080/realms/kafka/protocol/openid-connect/certs"
kafka.sasl.oauthbearer.token.endpoint.url="http://keycloak:8080/realms/kafka/protocol/openid-connect/token"
kafka.sasl.oauthbearer.expected.audience="account"
kafka.sasl.oauthbearer.client.id="myclient"
kafka.sasl.oauthbearer.client.secret="mysecret"

# JAAS Config
kafka.opts="-Djava.security.auth.login.config=/etc/kafka/secrets/kafka.server.jaas.conf"
kafka.listener.name.oidc.oauthbearer.sasl.server.callback.handler.class="org.apache.kafka.common.security.oauthbearer.OAuthBearerValidatorCallbackHandler"
```

For more information about Kafka SASL/OAUTHBEARER configuration, see the [official documentation](https://docs.confluent.io/platform/current/security/authentication/sasl/oauthbearer/configure-clients.html).

### Keycloak Configuration

Set up a Kafka realm in Keycloak with the following configuration:

1. Create a new realm named `kafka`
2. Create clients for:
    - EDC Provider: `edc-provider`
    - EDC Consumer: `edc-consumer`
    - Kafka Broker: `kafka-broker`
3. Configure client scopes:
    - `kafka-read`: For reading from topics
    - `kafka-write`: For writing to topics
4. Set up roles:
    - `kafka-admin`: For administrative access
    - `kafka-producer`: For producing messages
    - `kafka-consumer`: For consuming messages

## Security

### Authentication

The Kafka extension uses OAuth for authentication, which provides:
- Token-based authentication
- Fine-grained access control
- Token expiration and refresh

### Authorization

Authorization is managed through:
1. **EDC Policies**: Control who can access which assets
2. **Kafka ACLs**: Control which topics and operations are allowed
3. **OAuth Scopes**: Define the permissions granted to clients

### Credential Management

Credentials are securely managed through:
1. **Dynamic Provisioning**: Credentials are created on-demand for each transfer
2. **Secure Storage**: Credentials are stored in a secure vault
3. **Automatic Cleanup**: Credentials are removed when transfers are terminated

## Troubleshooting

### Common Issues

1. **Connection Issues**:
    - Check network connectivity between components
    - Verify Kafka broker is running and accessible
    - Ensure bootstrap servers are correctly configured

2. **Authentication Issues**:
    - Verify OAuth client credentials
    - Check token endpoint URLs
    - Ensure Keycloak is properly configured

3. **Authorization Issues**:
    - Check ACLs in Kafka
    - Verify OAuth scopes
    - Review EDC policies
