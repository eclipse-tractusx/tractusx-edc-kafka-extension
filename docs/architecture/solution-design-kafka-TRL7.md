# Solution Design – Tractus-X EDC Extension: Kafka Pull (TRL 3 → TRL 7)

## 1. Business Case

The Tractus-X EDC Kafka Extension enables policy-based, event-driven data exchange between sovereign partners via Apache Kafka. 
Advancing the current proof-of-concept (TRL 3) to TRL 7 will demonstrate a secure, interoperable, and end-to-end streaming data plane component for Tractus-X.

## 2. Requirements Analysis

### 2.1 Goals & Deliverables

#### 2.1.1 Solution Concept

- Design a robust architecture for the Kafka-based Streaming Data Plane
- Plan phased evolution from experimental prototype (TRL 3) to operational system (TRL 7)

#### 2.1.2 Implementation

- Develop a functional Kafka-based Data Plane extension
- Integrate with Eclipse Dataspace Connector (EDC)
- Ensure compliance with Tractus-X Release Guidelines (TRGs) for R25.12
- Conduct integration testing and document system behavior
- Provide technical documentation for configuration, installation, and operation

#### 2.1.3 EDC Streaming E2E Test

- Identify and specify relevant user journeys (e.g. asset publishing, contract negotiation)
- Derive and define test cases from the user journeys
- Implement automated E2E tests based on the existing CI/CD pipeline
- Set up the test environment using HELM charts (EDC, Kafka, Keycloak)
- Execute and document test runs (logs, validation results)
- Integrate testing into the automated pipeline and provide support for error analysis

#### 2.1.4 Development of a Use Case Demonstrator

- Implement a demonstrator with a local deployment showcasing a realistic use case
- Provide HELM charts for quick, consistent deployment
- Integrate test data to visualize streaming asset exchange
- Document configuration steps and commissioning procedures

### 2.2 Premises

- Apache Kafka serves as the streaming backbone
- Kubernetes is the target runtime (K3s, Kind, or cloud-native)
- GitOps approach with ArgoCD; CI/CD managed via GitHub Actions
- Open-source (Apache 2.0) with SPDX-compliant dependencies
- Alignment with Eclipse Tractus-X and Catena-X

### 2.3 Requirements Catalog

#### Functional

- Stream messages over Kafka based on policy-negotiated contracts
- Automate contract negotiation on event-based triggers
- Support Kafka subscription and publishing as part of the EDC Data Plane

#### Non-Functional

- Fault tolerance with retries, DLQs, and back-pressure support (? how far do we really need to go here?)
- Full observability: metrics, logging
- Secure identity and access control using OAuth2 and SSL
- Configurable deployment with HELM

#### Compliance

- Full alignment with Tractus-X Release Guidelines (R25.12)
- SPDX headers and license documentation
- Conformance to Data Space Protocol (DSP) specifications

## 3. Current Situation

### 3.1 Process

#### 3.1.1 Identified User Journeys

- Provider creates EDC asset for a topic → publishes messages to the topic
- Consumer negotiates contract offer for a topic → subscribes to the topic → message delivery

#### 3.1.2 Current System Capabilities

- Functional PoC exists with basic Kafka-to-EDC integration
- No Helm charts or Kubernetes-native configuration
- Missing error handling and observability
- Manual testing; lacks integration and end-to-end test automation and deployment reproducibility

### 3.2 Data

#### 3.2.1 Message Model Overview

- Kafka topics used for streaming messages
- JSON payloads exchanged without schema enforcement

#### 3.2.2 State Management

- Data exchange lifecycle follows the EDC state machine
- Status: `ContractNegotiation.REQUESTED`, `ContractNegotiation.FINALIZED`, `TransferProcess.REQUESTED`, `TransferProcess.STARTED`

#### 3.2.3 Protection & Risk

- Current setup lacks topic-level access control
- OAuth role mappings and token validation are not tested
- Kafka traffic is not encrypted (switch from SASL_PLAINTEXT to SASL_SSL)

### 3.3 System

#### 3.3.1 Context View

System diagram includes:

- EDC Core + Extension
- Kafka Broker
- OAuth2 Provider (Keycloak)
- Kafka Producer/Consumer apps

```mermaid
flowchart LR
    subgraph "Consumer Infrastructure"
        Consumer["Kafka Consumer App"]
        EDC_Consumer["EDC + Kafka Extension"]
        
        EDC_Consumer -.-> |"Passes token from EDR"| Consumer
    end
    
    subgraph "Provider Infrastructure"
        Producer["Kafka Producer App"]
        EDC_Provider["EDC + Kafka Extension"]
        Keycloak["OAuth2 Provider<br/>(Keycloak)"]
        Kafka["Kafka Broker"]
        
        Producer --> |"Publishes data"| Kafka
        Producer --> |"Gets token"| Keycloak
        EDC_Provider --> Kafka
        EDC_Provider --> |"Requests token"| Keycloak
    end
    
    EDC_Consumer --> |"Negotiates contract"| EDC_Provider
    EDC_Provider --> |"Sends EDR (incl. token, endpoint)"| EDC_Consumer
    Consumer --> |"Pulls data using token"| Kafka
    Kafka --> |"Delivers messages"| Consumer
```

#### 3.3.2 Interfaces

- REST APIs (EDC)
- Kafka topics with custom messages
- DataAddress definitions for Kafka endpoints

## 4. Target Architecture

### 4.1 High-Level Architecture

#### 4.1.1 System Landscape

- Full deployment on Kubernetes using Helm
- Modular EDC Extension with isolated Kafka communication logic
- Kafka provisioned via Strimzi or compatible deployment

Managed Kafka Operator via 
- [Strimzi](https://strimzi.io/) Helm Chart https://github.com/strimzi/strimzi-kafka-operator/tree/main/helm-charts/helm3/strimzi-kafka-operator 
- Bitnami Kafka Helm Chart https://bitnami.com/stack/kafka/helm

##### Kafka on Kubernetes: Strimzi vs Bitnami Helm Chart

| Feature                        | Strimzi (via Helm Chart)                           | Bitnami Kafka Helm Chart                     |
|--------------------------------|----------------------------------------------------|----------------------------------------------|
| **Type**                       | Operator-based                                     | Plain Helm Chart (no operator)               |
| **Helm Usage**                 | Installs the Strimzi Operator                      | Direct Kafka deployment via Helm             |
| **Kafka Lifecycle Management** | CRDs for Kafka, Topic, User, etc.                  | Manual configuration updates                 |
| **Security Features**          | TLS, mTLS, OAuth2, SCRAM, ACLs                     | TLS, basic authentication, SASL, ACLs        |
| **Scalability**                | Dynamic broker scaling via CRDs                    | Manual Helm upgrade or replica change        |
| **Observability**              | Prometheus/Grafana, Cruise Control, Kafka Exporter | Prometheus/Grafana support                   |
| **Kafka Connect, MirrorMaker** | Built-in CRDs                                      | Optional via subcharts or manual setup       |
| **Rolling Updates**            | Automated                                          | Manual (via Helm upgrade)                    |
| **Cloud Native Integration**   | Deep Kubernetes-native APIs (CRDs, RBAC)           | Kubernetes-native, but no CRDs               |
| **Complexity**                 | Higher (learning curve for CRDs and Operator)      | Lower (standard Helm practices)              |
| **Best For**                   | Production-grade, complex or dynamic environments  | Simpler, lightweight or dev/test deployments |
| **License**                    | Apache 2.0                                         | Apache 2.0                                   |

> ✅ **Recommendation**: Use **Strimzi** for production clusters requiring full lifecycle automation. Use **Bitnami** for simpler deployments or quick prototypes.

#### 4.1.2 Component Breakdown

The Kafka Extension consists of the following key components:

- **Kafka Broker Extension**: Core Control Plane extension that implements the `DataFlowController` interface to handle the "Kafka-PULL" transfer type. It manages the following operations:
  - Creating OAuth2 credentials and tokens for secure access to Kafka topics
  - Building Endpoint Data References (EDRs) for consumers
  - Revoking tokens and cleaning up credentials when transfers are suspended or terminated
  - Integrating with the EDC Vault for secure credential storage

- **Data Address Kafka**: Defines the data address format for Kafka assets, including:
  - Topic configuration
  - Bootstrap server settings
  - Security protocol and SASL mechanism
  - OAuth2 token configuration
  - Consumer group settings

- **Validator Data Address Kafka**: Validates Kafka data addresses to ensure they contain all required properties and have valid values.

- **OAuth Service**: Manages authentication and authorization for Kafka access:
  - Integrates with OAuth2 providers (e.g., Keycloak)
  - Handles token issuance, renewal, and revocation
  - Supports secure token-based access to Kafka topics

- **Monitoring Integration**: Provides observability through:
  - Metrics via Micrometer for performance monitoring
  - Logging via EDC Logging for troubleshooting
  - Integration with OpenTelemetry and Jaeger for visualization (https://github.com/eclipse-edc/Samples/tree/main/advanced/advanced-01-open-telemetry)

### 4.2 Technical Architecture

#### 4.2.1 Data Flow Design

- **Kafka Topics as Data Assets**: Kafka topics represent actual business data assets that can be shared between participants in a data space
- **Semantic Data Models**: Topics contain domain-specific business data in the form of semantic models that provide value to participants
- **Asset Registration**: Providers register Kafka topics containing valuable business data as assets in the EDC
- **Contract-Based Access**: Consumers gain access to these topic-based assets through the standard EDC contract negotiation process
- **Secure Data Exchange**: Upon successful contract negotiation, consumers receive secure access credentials (OAuth tokens) to consume data from the specific Kafka topics
- **Data Flow Traceability**: All data exchanges are tracked in the EDC with metadata including contractId, assetId, and timestamps to ensure auditability and compliance ℹ️ TBD how much of this is actually audited in the EDC

#### 4.2.2 Interface & Schema Definitions

- Extend `TransferStartMessage` to include `endpointProperties`
  - the EDC handles this internally. EDC management API does not return DataAddresses in the DSP Protocol form
- `DataAddress` must define Kafka broker URL via `endpoint` property
- Reference: [DSP 10.2.3 Transfer Start Endpoint](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1-RC1/#transfers-providerpid-start-post)

#### 4.2.3 Fault Tolerance

- Retry/backoff mechanism for failed transfers
- Support for idempotent processing (safely retry operations without unintended side effects)
- Chaos tests for Kafka and Keycloak failure scenarios
  - see chapter [6.1.4 Chaos & Load Testing](#614-chaos--load-testing)

#### 4.2.4 Observability

- Metrics via [Micrometer](https://eclipse-edc.github.io/documentation/for-contributors/metrics/)
- Logs via [EDC Logging](https://eclipse-edc.github.io/documentation/for-contributors/logging/)

### 4.3 Security Architecture

- Switch Kafka Security Mechanism from SASL_PLAINTEXT to SASL_SSL
- JWT/OAuth validation integrated into the Kafka flow
- Kafka topic ACLs bound to OAuth roles
- Enhanced test coverage for role-based access

#### SASL_SSL

The EDC Kafka Extension allows consumption of Topics over company borders. To allow secure transfer of data, the usage of end-to-end encryption is crucial. 
Hence, we need to switch from the security protocol from SASL_PLAINTEXT to SASL_SSL.

To achieve this on the client side, we simply replace the property. This way, all public CA-signed certificates are accepted.
We also want to allow flexibility, so it should be possible to configure the consumer client to use a custom certificate.

On the provider side, this requires more setup.
To sign and encrypt the traffic, the Broker needs to use an SSL Certificate. To not require additional exchange of trusted certificates, the Broker has to use a public CA-signed certificate. 

#### Token expiry

The current implementation uses the OAuth2 client_credentials Authentication flow. This is the recommended approach for machine-to-machine communication.
There is the downside that a token cannot be revoked once it has been issued.  
The consequence is that the provider cannot revoke access to a created Transfer process immediately. The token will always be valid for the remaining Token Expiry period.  
Subsequent transfer processes, however, will be denied afterward.
To limit the risk of unauthorized access to topics, the default Token TTL is set to 5 minutes.

It is technically possible to verify if a token was revoked, but this requires a custom LoginCallbackHandler, which is only possible by building a custom Docker image for the Kafka Broker. This is not recommended.

**Question: Do we want to allow a configurable Token Expiry – either via EDC configuration or via the Asset?**
This way, the provider can choose how long a token will be valid with the potential of a higher load on the identity provider due to more token issuance

#### Hybrid use of Kafka ACLs and OAuth2 roles

See ADR [hybrid-oauth2-kafka-acl-security](../adr/adr-hybrid-oauth2-kafka-acl-security.md)

## 5. Implementation & Deployment

### 5.1 DevOps & CI/CD

#### 5.1.1 Tooling Stack

- GitHub Actions for building, testing, and image publishing
- Docker Registry
- ArgoCD for GitOps deployment
- Helm for reproducible stack provisioning

#### 5.1.2 Pipeline Enhancements

- Integrate unit, integration, and E2E test stages
- Add software composition analysis and dependency checks
- Auto-publish Helm charts and Docker images
- Publish the package for the EDC extension to maven central
  - use tractus-x EDC repo as a template on how to publish maven packages

### 5.2 Guidelines

- Maintain changelog and ADRs
- Follow semantic versioning
- Align codebase with Tractus-X linting and release conventions

## 6. Testing & TRL Validation

### 6.1 Testing Strategy

#### 6.1.1 Unit Testing

- Expand unit test coverage in EDC Kafka extension
- Add edge cases and improve assertions
- Introduce unit tests in the use case demonstrator apps

#### 6.1.2 Integration Testing

- Testcontainers-based integration setup simulating real Kafka interaction
- EDC-like Testcontainer orchestration for full message flow simulation https://eclipse-edc.github.io/documentation/for-contributors/testing/#4-integration-tests
- Focus on resilience and failure handling

- Is it possible to test role-based access on the integration level?

#### 6.1.3 End-to-End Testing

- Helm-based E2E tests in Kubernetes
- Validate complete asset negotiation and transfer via Kafka
- Target: Participation in R25.12 E2E Testing Phase

##### Test Cases

###### Good Cases

- provider creates an asset → provider publishes a message → consumer negotiates the topic → consumer consumes a message
- provider creates an asset → provider publishes a message → consumer negotiates the topic → consumer consumes a message → consumer consumes until token expiry → consumer refreshes the transfer process → consumer continues to consumer messages

###### Bad Cases

- provider creates an asset → provider publishes a faulty message → consumer negotiates the topic → consumer displays the correct error
- provider creates asset → provider publishes a message → consumer negotiates the topic → negotiation fails
- provider creates asset → provider publishes a message → consumer negotiates the topic → provider terminates contract → consumer tries to consume a message → consumption fails
- provider creates asset → provider publishes a message → consumer negotiates the topic → consumer tries to access a different topic with token → access is denied

#### 6.1.4 Chaos & Load Testing

- Simulate outages for Kafka and Keycloak
- Determine the need and scope for load testing based on demonstrator results

##### Test Cases

- Keycloak unavailable
- Kafka Broker unavailable

#### 6.1.5 Test Data Management

- Evaluate existing test data quality
- Use pseudo-randomly generated semantic assets (e.g., semantic BOM)
- Define reproducible datasets for regression testing

### 6.2 TRL Roadmap

| TRL | Objective                              | Milestone                                   |
|-----|----------------------------------------|---------------------------------------------|
| 3   | Prototype runs locally                 | Manual testable PoC                         |
| 4   | Interfaces work in containerized tests | Integration with Kafka validated            |
| 5   | E2E flow completed                     | Negotiation + message delivery automated    |
| 6   | Kubernetes deployment                  | CI/CD pipeline, secured components          |
| 7   | Demonstrator with full coverage        | Tractus-X compliant, observable, and tested |

## 7. Demonstrator & Use Case

### 7.1 Demonstrator Architecture

- Clean, focused use-case with business relevance
- Implemented via Spring Boot microservices (Kafka Producer & Consumer)
- Consider publishing as Docker images for use in Helm
- Do we need to complete the transfer processes once the token is expired? 
  - Can this be done within the EDC?
  - Is it possible to do this in the EdrTokenCallbackHandler of the Consumer App?

### 7.2 Helm Deployment

- Modular Helm charts:
  - `edc-core` (EDC + extension)
  - `infrastructure` (Kafka, Keycloak)
  - `provider` and `consumer` setup split
  - Optional: separate charts for Kafka Producer/Consumer Apps

### 7.3 Documentation

- Solution design and demonstrator architecture in Markdown
- Setup instructions and commissioning guide
- EDC extension configuration examples

## 8. Documentation & Compliance

### 8.1 Open Source Compliance

- SPDX headers in all source files
- Apache 2.0 license for Code included
- CC-BY-4.0 license for non-Code included
- 3rd-party dependency tracking (SBOM)

### 8.2 Conformance to Tractus-X Standards

- Aligned with EDC conformance profiles
- Tractus-X Release Guidelines (R25.12) satisfied
- Kafka Data Transfer Profile to be added (send to Arno Weiss)
  See: [DSP Data Transfer Profiles](https://github.com/eclipse-dataspace-protocol-base/dsp_best_practices/tree/main/profiling/data-transfer-profiles)

### 8.3 Documentation Assets

- Architecture Decision Records (ADRs)
- API documentation (OpenAPI, JSON schema)
- Testing and release documentation

## 9. Project Management

### 9.1 Stakeholder Analysis

- Tractus-X release coordinators
- Arno Weiss (reviewer for transfer profiles)
- Lars Geyer-Blaumeiser (Lead Tractus-X EDC, TAP7.8 Lead)

### 9.2 Open Items List and Decision Log

- Do we need a schema registry?
- Do we want to allow custom configurable token expiry?
- Do we need Kafka ACLs tied to OAuth roles?
- How much do we need to implement in terms of fault tolerance with retries, DLQs, and back-pressure support?
  - Where would the right place for implementation be?
  - Is this applicable for the EDC extension if we don't proxy any kafka traffic? 
  - Do we need to implement this in the consumer/provider apps?
- Do we want to publish the consumer/provider apps as part of this repo?
  - should they be only used for testing?
  - do we want dedicated helm charts for the apps?

### 9.3 Risk List

- Compatibility with evolving Tractus-X release guidelines (R25.12)

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

* SPDX-License-Identifier: CC-BY-4.0
* SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
* Source URL: <https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension>
