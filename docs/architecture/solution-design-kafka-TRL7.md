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
- Status: `NegotiationStarted`, `ContractAgreed`, `TransferStarted`, `TransferCompleted`

#### 3.2.3 Protection & Risk

- Current setup lacks topic-level access control
- OAuth role mappings and token validation not tested
- Kafka traffic not encrypted (switch from SASL_PLAINTEXT to SASL_SSL)

### 3.3 System

#### 3.3.1 Context View

System diagram includes:

- EDC Core + Extensions
- Kafka Broker
- OAuth2 Provider (Keycloak)
- Kafka Producer/Consumer apps

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

#### 4.1.2 Component Breakdown

- **Kafka Adapter**: Handles subscription and publishing
- **Contract Manager**: Orchestrates contract lifecycle
- **Policy Validator**: Enforces access restrictions
- **Monitoring Agent**: Integrates with Prometheus, Grafana, and Loki

### 4.2 Technical Architecture

#### 4.2.1 Event Flow Design

- Topic mapping: `asset.published`, `contract.status`, `transfer.completed`
- Events enriched with metadata (contractId, assetId, timestamps)

#### 4.2.2 Interface & Schema Definitions

- Extend `TransferStartMessage` to include `endpointProperties`
- `DataAddress` must define Kafka broker URL via `endpoint` property
- Reference: [DSP 10.2.3 Transfer Start Endpoint](https://eclipse-dataspace-protocol-base.github.io/DataspaceProtocol/2025-1-RC1/#transfers-providerpid-start-post)

#### 4.2.3 Fault Tolerance

- Retry/backoff mechanism for failed transfers
- Support for idempotent processing (safely retry operations without unintended side effects)
- Chaos tests for Kafka and Keycloak failure scenarios

#### 4.2.4 Observability

- Metrics via [Micrometer](https://eclipse-edc.github.io/documentation/for-contributors/metrics/)
- Logs via [EDC Logging](https://eclipse-edc.github.io/documentation/for-contributors/logging/)

### 4.3 Security Architecture

- Switch Kafka Security Mechanism from SASL_PLAINTEXT to SASL_SSL
- JWT/OAuth validation integrated into the Kafka flow
- Kafka topic ACLs bound to OAuth roles
- Enhanced test coverage for role-based access

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

#### 6.1.3 End-to-End Testing

- Helm-based E2E tests in Kubernetes
- Validate complete asset negotiation and transfer via Kafka
- Target: Participation in R25.12 E2E Testing Phase

#### 6.1.4 Chaos & Load Testing

- Simulate outages for Kafka and Keycloak
- Determine need and scope for load testing based on demonstrator results

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

### 9.3 Risk List

- Compatibility with evolving Tractus-X release guidelines (R25.12)

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

* SPDX-License-Identifier: CC-BY-4.0
* SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
* Source URL: <https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension>
