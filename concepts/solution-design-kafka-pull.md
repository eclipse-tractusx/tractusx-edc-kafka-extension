# Kafka pull solution design

## Requirements

| ID      | Requirement                                                                                        | Description                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                        | Deliverables                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
|---------|----------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 7.8.1.1 | Coordination with BMW and Cofinity-X for Clarification of Infinit Provider Push and Kafka Solution | For further optimization and implementation of the **BMW Infinit Provider Push** solution as well as the **Cofinity-X Kafka solution**, targeted coordination meetings should be held with the relevant stakeholders from BMW and Cofinity-X. The goal is to gain a detailed understanding of both solutions, clarify technical challenges, and define a consolidated approach for integration.                                                                                                                                                                                                                                                                                                                    | Meetings with the responsible parties from **BMW (Infinit Provider Push)** and **Cofinity-X (Kafka solution)** have been successfully held.  <br/> Clarification and documentation of the respective **technical concepts, architectures, and interfaces**.                                                                                                                                                                                                                                                                                                                                                                   |
| 7.8.1.2 | Migration of the Closed Source Repository to Tractus-X                                             | The current PoC implementation is closed source. It needs to be migrated to a Tractus-X repository.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                | PoC is available in Tractus-X [tractusx-edc-kafka-extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension) <br/> Open Source Governance has been carried out <br/> Repository quality is appropriate.                                                                                                                                                                                                                                                                                                                                                                                                     |
| 7.8.1.3 | Analysis and Evaluation of the PoC                                                                 | For the extension of the DataPlane of the Tractus-X EDC, a Proof of Concept (PoC) should be developed that enables support for a streaming-capable protocol (Apache Kafka). The goal is to facilitate real-time data exchange at the data plane between data providers and consumers, and to make participation in the Catena-X data space more efficient and accessible.                                                                                                                                                                                                                                                                                                                                          | Analysis and evaluation of the PoC to identify optimization potential <br/> Optimization potentials are documented in the repository [tractusx-edc-kafka-extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension).                                                                                                                                                                                                                                                                                                                                                                                        |
| 7.8.1.5 | Identification and Definition of Use Cases                                                         | The relevant use cases for the Kafka Streaming Data Plane Extension have been described. **Note: "Requirements - what use cases need these"**                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | Identification and definition of the use cases for which the Kafka EDC Data Plane can be used. Which use cases from CATENA-X can benefit from it. (10 core use cases and their sub use cases) <br/> e.g.: <br/> Real-time shop floor control <br/> Real-time shop floor monitoring <br/> Support for notification use cases <br/> 3-5 use cases are identified and documented in the repository.                                                                                                                                                                                                                              |
| 7.8.2.1 | Open Source Quality Assurance                                                                      | Implementation of Open Source Governance to manage the repository in the Eclipse Foundation & Tractus-X. <br/> The open-source software must adhere to defined quality standards and best practices to ensure high code quality, security, maintainability, and compliance.                                                                                                                                                                                                                                                                                                                                                                                                                                        | The repository [tractusx-edc-kafka-extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension) must meet the requirements according to TRG 7 - Open Source Governance. <br/> TRG 7 - Open Source Governance is fulfilled.                                                                                                                                                                                                                                                                                                                                                                                    |
| 7.8.3.2 | Data Access for the Consumer                                                                       | Streaming-capable protocol                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | The **data provider (Provider)** must be able to continuously provide data via a **streaming-capable protocol**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                              |
| 7.8.3.3 | Data Access for the Consumer                                                                       | Provision via Eclipse Dataspace Connector (EDC).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   | The provided data must be made available through the **Eclipse Dataspace Connector (EDC)**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                   |
| 7.8.3.4 | Data Access for the Consumer                                                                       | Provider makes asset available via a catalog.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                      | The **provider provides an asset in the catalog** and defines **usage conditions** (Contract Policy).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 7.8.3.5 | Data Access for the Consumer                                                                       | Data consumer (Consumer) via EDC.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  | The **data consumer (Consumer)** must have the ability to connect to the **provider's EDC** and retrieve the data stream.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                     |
| 7.8.3.6 | Data Access for the Consumer                                                                       | Persistent connections between consumer and provider                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               | The protocol must support **persistent connections**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         |
| 7.8.3.7 | Data Access for the Consumer                                                                       | Initiation of the data transfer via an API                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                         | The **initiation of the data transfer is done via an API** (e.g. using Insomnia or Postman).                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                  |
| 7.8.3.8 | Support for the Streaming Protocol Kafka                                                           |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | The **data streaming transfer** must be carried out using the **Kafka** (Pub/Sub messaging for real-time data streams) **protocol**.                                                                                                                                                                                                                                                                                                                                                                                                                                                                                          |
| 7.8.4.1 | Showcase – Streaming-capable Data Consumption via EDC with Defined Use Cases                       |                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                    | As part of a demo, it should be shown that a data consumer (Consumer) can consume data from a data provider (Provider) via a streaming-capable protocol using the Eclipse Dataspace Connector (EDC). <br/> The setup of the demo includes: <br/> Provision of a data provider and data consumer <br/> Data consumption using the streaming protocol (Kafka) <br/> Use of Insomnia to interact with the data consumer and data provider <br/> Two specific use cases will be demonstrated <br/> Demo is carried out based on test data.                                                                                        |
| 7.8.5.1 | Proof of Processing Semantic Models in accordance with CX-0127 Industry Core: Part Instance 2.0.0  | The solution must ensure full compatibility with existing Catena-X interfaces and data models. This includes technical and semantic interoperability so that data exchange can occur seamlessly within the Catena-X ecosystem. <br/> Additionally, it must be demonstrated that the semantic models according to CX-0127 Industry Core: Part Instance 2.0.0 can be correctly processed. This is achieved through the implementation, validation, and testing of the relevant data models, including SerialPart, Batch, JustInSequencePart, SingleLevelBomAsBuilt, and SingleLevelUsageAsBuilt. Verification is carried out based on defined test cases and scenarios to ensure compliance with Catena-X standards. | It must be ensured that the semantic models from the repository [eclipse-tractusx/sldt-semantic-models](https://github.com/eclipse-tractusx/sldt-semantic-models/) can be processed according to the requirements of CX-0127 Industry Core: Part Instance 2.0.0. This includes, in particular, support for the following aspect models: <br/> SerialPart <br/> Batch <br/> JustInSequencePart <br/> SingleLevelBomAsBuilt <br/> SingleLevelUsageAsBuilt <br/> Proof is provided through the development and execution of specific test cases and scenarios that validate the full integration and processing of these models. |
| 7.8.6.1 | Standard Compatibility                                                                             | The solution must ensure that interoperability and standard compliance according to Catena-X requirements are guaranteed. This pertains to both the technical integration with existing Catena-X components and adherence to the defined data, API, and security standards.                                                                                                                                                                                                                                                                                                                                                                                                                                        | The solution is developed by the team according to the requirements of the standard https://catenax-ev.github.io/docs/standards/CX-0018-DataspaceConnectivity. <br/> The dS team verifies the solution according to the standard https://catenax-ev.github.io/docs/standards/CX-0018-DataspaceConnectivity. <br/> The verification is documented in the repository.                                                                                                                                                                                                                                                           |

## Problem Statement

The Tractus-X EDC currently lacks a dedicated, real-time data streaming mechanism that enables continuous, event-driven
communication between data providers and consumers. This Proof of Concept (POC) integrates Apache Kafka as a
decentralized component on the provider side, operating behind the Eclipse Dataspace Connector (EDC) to evaluate a
publish/subscribe-based streaming solution that facilitates real-time machine and production data exchange on the
automotive shop floor, thereby evaluating the necessary protocol extensions, integration challenges, and risks before
full-scale adoption.

## Example

Consider an automotive shop floor where production machines continuously generate data on operational status,
performance metrics, and production counts. Each company retains control over its own data sink and connects it to the
Tractus-X network via the Eclipse Dataspace Connector, preserving data sovereignty. Within this framework, a
decentralized Kafka Broker and Kafka Provider at the provider’s site manage the real-time publication of data, published
as EDC Offer, while on the consumer side, individual Kafka Consumers consume the Offer, subscribe to the data stream to
trigger immediate alerts and enable dynamic adjustments in production processes, thereby optimizing efficiency and
reducing downtime.

## Use Cases

| Use Case                                      | Description                                                                                                                                                                                                                                                                                                                                                          |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Real-time Operational Monitoring and Analysis | This use case encompasses the continuous real-time monitoring of production data, quality metrics, environmental parameters, and supply chain information. It enables immediate analysis and response to operational events, fostering optimized process control, proactive decision-making, and enhanced transparency across manufacturing and logistics processes. |
| Predictive Maintenance and Data Integration   | This use case focuses on leveraging streaming data to identify maintenance needs early while integrating external data sources to support informed decision-making. It helps minimize downtime through predictive maintenance and improves adaptability by incorporating external factors such as weather conditions and market trends.                              |

## Architectural Overview

### Key Components

The architecture is divided into three principal areas: the Provider ecosystem, the Provider cluster, and the Consumer
ecosystem/cluster. The PlantUML component diagram explicitly illustrates these divisions:

- **Provider Ecosystem:**
    - **Provider Application:** Initiates the data production process by sending a stream of data to Kafka through a
      dedicated Producer API.

- **Provider Cluster:**
    - **Control Plane:** Orchestrates the transfer by managing negotiation, policy checks, and interaction with the
      Kafka Extension.
    - **Data Plane:** Implements the runtime data transfer logic, including token renewal and DataAddress generation.
    - **Kafka Extension:**
        - Acts as the mediator between the control and data planes.
        - Orchestrates dynamic credential provisioning/deprovisioning.
        - Communicates with the OAuth Service via an OAuth Management API for creating, renewing, and revoking tokens.
        - Reads from and writes to a secure Vault where temporary credentials are stored.
    - **Kafka Service:** Provides the underlying messaging platform. It enforces authentication (via
      SASL/OAUTHBEARER) and authorization on topics.
    - **OAuth Service:**
        - Handles dynamic client registration and token management.
        - Issues access and refresh tokens.
    - **Vault:** Secure repository for storing temporary credentials and secrets.
    - **OAuth Manage API:** The interface through which the Kafka Extension and Data Plane manage dynamic OAuth
      credentials.
- **Consumer Ecosystem/Cluster:**
    - **Consumer Application:** Consumes data by subscribing to the Kafka topic.
    - **Consumer Control Plane:** Initiates data transfer requests and receives the Endpoint Data Reference (EDR)
      necessary to create a secure connection to Kafka.
    - **Consumer API (via Kafka Consumer):** Directly interacts with the Kafka Service to poll data once authenticated.

[Component diagram of using KafkaBroker](/concepts/MVP-kafka-pull/puml/Component%20diagram%20EDC%20Kafka%20Extension.puml):
![Component diagram EDC Kafka Extension.png](/concepts/MVP-kafka-pull/png/Component%20diagram%20EDC%20Kafka%20Extension.png)

## Workflow and Process Phases

The system processes are defined by four key phases. Each phase is illustrated by its corresponding sequence diagram,
which provides a step-by-step message flow.

### A. Provisioning/Deprovisioning Phase

**Purpose:** Securely create or delete consumer credentials.

**Flow:**

1. **Provisioning:**
    - The **Consumer Control Plane** sends a **TransferRequestMessage** to the **Provider Control Plane**.
    - The **Provider Control Plane** forwards a **ProvisionRequest** to the **Kafka Extension**.
    - The **Kafka Extension** calls the **OAuth Service** to create new credentials.
    - Once credentials are issued, the Kafka Extension saves them securely in the Vault.
    - A **ProvisionResponse** is then returned from the Kafka Extension up through the Provider Control Plane back to
      the Consumer.
2. **Deprovisioning:**
    - When the transfer concludes, the **Provider Control Plane** issues a **DeprovisionRequest** to the Kafka
      Extension.
    - The Kafka Extension instructs the **OAuth Service** to delete the credentials.
    - After credential deletion, the Kafka Extension clears the corresponding entries from the Vault and returns a *
      *DeprovisionResponse**.

[Sequence diagram of provisioning/deprovisioning using Kafka Extension](/concepts/MVP-kafka-pull/puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20provisioning-deprovisioning.puml):

![Sequence diagram EDC Kafka Extension provisioning-deprovisioning.png](/concepts/MVP-kafka-pull/png/Sequence%20diagram%20EDC%20Kafka%20Extension%20provisioning-deprovisioning.png)

### B. Initiating the Transfer

**Purpose:** Initiate a transfer process with dynamic credentials and EDR creation.

**Flow:**

1. **Start Transfer Request:**
    - The **Consumer Control Plane** instructs the **Provider Control Plane** to start the transfer process.
    - The Provider Control Plane performs policy and contract verification.
2. **DataFlowRequest:**
    - The Provider Control Plane sends a **DataFlowRequest** to the Kafka Extension.
    - The Kafka Extension retrieves credentials from the Vault.
    - It then communicates with the **Data Plane** to initiate the transfer.
    - The Data Plane contacts the **OAuth Service** to generate both an access token and a refresh token.
    - After token generation, the Data Plane creates a **DataAddress** containing necessary connection details (such as
      topic, poll duration, OAuth token, and groupPrefix).
    - A **DataFlowResponse** message is returned to the Kafka Extension, which adjusts the DataAddress if needed and
      passes it back to the Provider Control Plane.
    - Finally, the Provider Control Plane sends a **TransferStartMessage** (attached with the complete DataAddress) to
      the Consumer Control Plane.
    - The Consumer Control Plane then constructs the final Endpoint Data Reference (EDR) for use by the consumer
      application.

[Sequence diagram of start transfer process](/concepts/MVP-kafka-pull/puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20start%20transfer%20process.puml):

![Sequence diagram EDC Kafka Extension start transfer process.png](/concepts/MVP-kafka-pull/png/Sequence%20diagram%20EDC%20Kafka%20Extension%20start%20transfer%20process.png)

### C. Data Streaming Phase

**Purpose:** Establish a secure, token-based data stream between the consumer and Kafka Service.

**Flow:**

1. **EDR Request and Authentication:**
    - The **Consumer Application** requests an EDR from the **Consumer Control Plane**.
    - Upon receipt of the EDR, the Consumer Application uses it to instantiate a Kafka Consumer.
    - The consumer then initiates authentication with the **Kafka Service**.
    - The Kafka Service, in turn, verifies the token by communicating with the **OAuth Service**.
    - After successful authentication, the consumer begins polling the Kafka Service for data messages.
2. **Continuous Data Polling:**
    - As long as the token remains valid, the consumer continuously polls and processes data messages from the Kafka
      Service.
    - The token’s life-cycle is managed automatically via one of the configured refresh strategies.

[Sequence diagram of data streaming](/concepts/MVP-kafka-pull/puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20data%20streaming.puml):

![Sequence diagram EDC Kafka Extension data streaming.png](/concepts/MVP-kafka-pull/png/Sequence%20diagram%20EDC%20Kafka%20Extension%20data%20streaming.png)

### D. Suspending/Terminating the Transfer

**Purpose:** Securely suspend or terminate data transfer by revoking consumer credentials.

**Flow:**

1. Initiation of Termination:
    - The Provider Control Plane sends a Suspend/Terminate Message to the Kafka Extension.
2. Token Revocation:
    - The Kafka Extension forwards a revoke token instruction to the Data Plane.
    - The Data Plane then contacts the OAuth Service to revoke the tokens.
    - Once the OAuth Service confirms revocation, the Data Plane notifies the Kafka Extension.
    - The Kafka Service is informed, ensuring that polling stops for the revoked token, and the overall termination is
      acknowledged back to the Provider Control Plane.
    - Finally, the Consumer Control Plane is informed that the transfer has been suspended or terminated.

[Sequence diagram suspending/terminating using KafkaBroker](/concepts/MVP-kafka-pull/puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20suspending-terminating.puml):

![Sequence diagram EDC Kafka Extension suspending-terminating.png](/concepts/MVP-kafka-pull/png/Sequence%20diagram%20EDC%20Kafka%20Extension%20suspending-terminating.png)

## Security and Token Management

### Token Creation and Expiration

- **Dynamic Token Issuance:**
  The OAuth Service provides short-lived access tokens and refresh tokens using SASL/OAUTHBEARER. These tokens guarantee
  that any consumer’s access is temporary and can be refreshed or revoked as needed.
- **Secure Vault Storage:**
  All dynamically issued credentials are saved within a secure Vault by the Kafka Extension. This allows both safe
  retrieval during transfer start and secure cleanup upon transfer termination.

### Token Refresh Strategies

The design supports various token refresh strategies:

- Automatic Refresh via Consumer DataPlane:
  Proactively refresh tokens before they expire.
- Automatic Refresh via the EDR API (/edrs):
  The Provider Control Plane can handle token refresh operations when needed.
- Manual Refresh:
  The consumer application can trigger token refresh operations if required.

These strategies ensure that the system can maintain uninterrupted data flow while complying with dynamic security
policies.

See the [Tractus-X EDC Signaling Extension](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/development/dataplane-signaling/tx-signaling.extensions.md)
for more details.

## Interoperability

The Kafka Extension does not change anything related to IATP, DSP and policy definitions. This ensures full
conformity to the
Standard [CX-0018 Dataspace Connectivity v.3.1.0](https://catenax-ev.github.io/docs/standards/CX-0018-DataspaceConnectivity)
for chapters 2.1, 2.3, 2.4 and 2.5.

Since the Kafka PULL extension will introduce the new transfer type `Kafka-PULL` the standard has to be extended by this
new type once the extension exits POC state and is introduced as a proper Tractus-X EDC extension.

An example for the extended standard could be:

> 2.2.3 Kafka-PULL
> 
> A Consumer MUST send a `dspace:TransferRequestMessage` with `dct:format:dspace:Kafka-PULL`.
> 
> A Provider MUST send a `dspace:TransferStartMessage` with sufficient information in the `dspace:dataAddress` property so that a client connection to the `dspace:endpoint` may succeed when initialized with the properties `scopes`, `groupPrefix` and `topic`.
> 
> A Provider Connector MUST ensure that the requested backend system has sufficient context from the negotiation to evaluate the legitimacy of the request.
> 
> A Consumer may then use the provided data to execute requests against the endpoint.
> 
> Despite the token, the endpoint still has the right to refuse serving a request. This may occur for instance when a consumer attempts to request for a different topic than the one specified in the `dspace:dataAddress`.

## Enhancements Over the Existing POC

The revised design improves upon the initial proof-of-concept by incorporating:

- **Dynamic Credential Management:** Replacing hard-coded credentials with OAuth-driven dynamic client registration and token management.
- **Robust Security and Token Lifecycle:** Implementing short-lived tokens with secure vault storage and supporting multiple token refresh strategies, thereby reducing exposure risks.

## NOTICE

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

* SPDX-License-Identifier: CC-BY-4.0
* SPDX-FileCopyrightText: 2025 Contributors to the Eclipse Foundation
* Source URL: <https://github.com/eclipse-tractusx/tractus-x-umbrella>
