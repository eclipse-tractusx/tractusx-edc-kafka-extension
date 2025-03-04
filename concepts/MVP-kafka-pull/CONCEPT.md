# KafkaBroker PULL Concept
# Kafka PULL Transfer MVP Concept

## Main Idea
Provide the Kafka PULL transfer type that allows consumers to subscribe specific data assets by leveraging Kafka's robust messaging infrastructure.

## General view of the Kafka PULL Transfer Workflow
To utilize all the benefits of Kafka's messaging infrastructure, this concept proposes direct data transfer between
the provider and consumer without routing data through the `Data Plane`.

The `Control Plane` will be responsible for managing credentials and controlling access to the Kafka infrastructure.
Therefore, the provider will have full control over the consumer's access throughout the entire transfer process.
This control will be achieved by new `Kafka Extension`.
Its primary functions will include orchestrating the creation/deletion of credentials, and obtaining/revoking
tokens (access and refresh) in the `OAuth Service` during all the transfer process.

Both the `Kafka Service` and the `OAuth Service` will be managed by the provider.
All interactions between the provider, the `OAuth Service` and the `Kafka Service`(such as credentials for sending data,
topics creating, mapping OAuth scopes to Kafka ACLs, etc.) are out the scope of the concept, as will be exclusively managed by the provider.

### Prerequisites
As a Kafka authentication mechanism the [SASL/OAUTHBEARER](https://kafka.apache.org/documentation/#security_sasl_oauthbearer) will be used.
This allows the use of OAuth tokens that align with the standard [RFC 7628](https://datatracker.ietf.org/doc/html/rfc7628)

To have the ability to create credentials for the consumer dynamically, an  `OAuth Service` that supports
Dynamic Client Registration (DCR) aligned with the [RFC 7591](https://datatracker.ietf.org/doc/html/rfc7591) protocol will be used.

Information about the topic, scopes, groupPrefix, SecretKey, etc., could be provided in the Asset.
Kafka endpoint, and the OAuth registration endpoint could be provided as configuration parameters.

For streaming/polling data, the available Kafka APIs `Provider API` and `Consumer API` will be provided externally
as well as the OAuth API  `OAuth Manage API` for managing dynamic credentials.
Provider's `Kafka Service` and `EDC Service` will be configured to use the same `OAuth Service`.

### Provisioning/Deprovisioning phase
During the provisioning/deprovisioning phase, the `Kafka Extension` will interact with the provider's `OAuth Service`
(using SecretKey, registration endpoint, scopes) and dynamically creates/deletes credentials for the consumer.
All credentials will be securely stored in the `Vault` for the duration of the active transfer process.

[Sequence diagram of provisioning/deprovisioning using Kafka Extension](puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20provisioning-deprovisioning.puml):

![Sequence diagram EDC Kafka Extension provisioning-deprovisioning.png](png/Sequence%20diagram%20EDC%20Kafka%20Extension%20provisioning-deprovisioning.png)

### Initiating the transfer
The consumer initiate the transfer processes and the provider's `Control Plane` sends a DataFlowRequest to the `Kafka Extension`.
The `Kafka Extension` interacts with the `Asset`, creates credentials, obtain the token, and based on this, generates the data address.

Once the transfer is successfully initiated, the consumer's `Control Plane` receives the data address along with all necessary
subscription information (e.g., topic, groupPrefix, token, etc.). This information will be utilized to construct the EDR.

[Sequence diagram of start transfer process](puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20start%20transfer%20process.puml):

![Sequence diagram EDC Kafka Extension start transfer process.png](png/Sequence%20diagram%20EDC%20Kafka%20Extension%20start%20transfer%20process.png)

### Poll data from kafka service
Using EDR, the consumer can directly instantiate a [`KafkaConsumer`](https://kafka.apache.org/37/javadoc/org/apache/kafka/clients/consumer/KafkaConsumer.html)
to subscribe to the `Kafka Service` and poll the data.

[Sequence diagram of data streaming](puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20data%20streaming.puml):

![Sequence diagram EDC Kafka Extension data streaming.png](png/Sequence%20diagram%20EDC%20Kafka%20Extension%20data%20streaming.png)

### Suspending/Terminating the transfer
Upon suspension or termination of the transfer, the `Kafka Extension` will revoke the consumer's token.
This ensures that consumers could no longer receive topic content.

[Sequence diagram suspending/terminating using KafkaBroker](puml/Sequence%20diagram%20EDC%20Kafka%20Extension%20suspending-terminating.puml):

![Sequence diagram EDC Kafka Extension suspending-terminating.png](png/Sequence%20diagram%20EDC%20Kafka%20Extension%20suspending-terminating.png)

### Token creation
To enhance transfer security, the EDR will provide a token with an expiration.
The token is issued by the `OAuth Service` used by Kafka for authentication and authorization.
A token for the consumer will be generated by the `Data Plane` by included `dataplane-token-refresh` extensions(which implement `DataPlaneAccessTokenService`).

### Refresh token
For refreshing the token, the `Data Plane` by included `tokenrefresh-handler` and `dataplane-token-refresh` extensions can be utilized.
These extensions support token refresh through four distinct methods:
1) Automatic refresh using the (consumer) DataPlane
2) Automatic refresh using the `/edrs` API
3) Manual refresh by the client application
4) Token refresh is handled completely out-of-band (without support from Tractus-X EDC)

For detailed information, please refer to the [Token Refresh mechanisms](https://github.com/eclipse-tractusx/tractusx-edc/blob/main/docs/development/dataplane-signaling/tx-signaling.extensions.md)

### Component diagram
[Component diagram of using KafkaBroker](puml/Component%20diagram%20EDC%20Kafka%20Extension.puml):

![Component diagram EDC Kafka Extension.png](png/Component%20diagram%20EDC%20Kafka%20Extension.png)

### DataAddress Schema

#### Properties

| Key                       | Description                                                                                                        | Mandatory |
|:--------------------------|:-------------------------------------------------------------------------------------------------------------------|-----------|
| `type`                    | Identifier of kafka data address.                                                                                  | `true`    |
| `topic`                   | Defines the Kafka topic                                                                                            | `true`    |
| `scopes`                  | Scopes for granting access to the topic                                                                            | `true`    |
| `pollDuration`            | Defines the duration of the consumer polling. The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds. | `false`   |
| `secretKey`               | Defines the `vault` entry containing the secret token/credentials                                                  | `true`    |
| `groupPrefix`             | Defines the groupPrefix that will be allowed to use for the consumer                                               | `true`    |


#### Secret Resolution

The `secretKey` property should point to a `vault` entry that contains a token to `OAuth Manage API`.

It will have permission to create additional credentials with defined scopes permissions.

#### Examples:

Asset Data Address:
```json
{
  "dataAddress": {
    "type": "KafkaBroker",
    "topic": "kafka-stream-topic",
    "pollDuration": "PT5M",
    "secretKey": "secretKey",
    "scopes": "read.kafka-stream-topic"
  }
}
```

EDR:
```json
{
  "@type": "DataAddress", 
  "scopes": "read.kafka-stream-topic",
  "groupPrefix": "bob",
  "type": "EDR",
  "endpoint": "localhost:9093",
  "contractId": "dd4796b1-3232-4156-9921-94afb0abd537",
  "topic": "kafka-stream-topic",
  "id": "acba6ff2-4502-46af-a3a4-10378b663b71", 
  "tx-auth:refreshEndpoint": "http://localhost:8585/public",
  "tx-auth:audience": "did:web:bob",
  "tx-auth:refreshToken": "eyJraWQiOiJwdWJsaWMta2V5IiwiYWxnIjoiUlMyNTYifQ.eyJleHAiOjE3MzgyMzQyNzcsImlhdCI6MTczODIzMDY3NywianRpIjoiMjA1MjY4ODAtN2VmYy00MjZmLTg4MDgtNzNmNTExYjdmYTg5In0.HpNKMHOmIqdec7AwCWewpD1etpI5h6_Wat6Y0qCq3ANs9eDwZHoiKF7iGJyqm7a6Dj7Qu3R4ryQtOHtze9bcE9Tx6A3APeuwuH9bckCgvEQQjKobwWJBzc6ju-V7Ru3p8Z25J8K70Me8PYgacbsHGM2MMrWcJv-vDsC5UlNJdkE7jA8JujhhRKX6NRN02C4LS3rPF3Gj8W_taiRCQqFhF91UtkoKH1VIKhzOSkJIVsy-smbTPBmvcGBIn4v5BwGR6UNbATFEDuT4-LXPtYhogkGAlgwidSAG6yWdzboRgMSxcxtXdu_FBVL7Rv-fm75kFn1_a4Qgcev_K9n2XcnB-SRxeil3zvUvBJ_G7_cyielaSds4hsmJHVs-UJ3trOP4KUir-KWf_l71vg0Ywb8Lb2gdmOph6qHL33TTTtWHQJyFguat0aHqUindzBbFPaj-UrIfVnRA4dwDHPPEbyWLt_6-tMx8ABGyai5WQYyy0AQTfbdSGJ_AGrSNq4g-aJRw",
  "tx-auth:expiresIn": "3600",
  "authorization": "eyJraWQiOiJwdWJsaWMta2V5IiwiYWxnIjoiUlMyNTYifQ.eyJpc3MiOiJhbm9ueW1vdXMiLCJhdWQiOiJib2IiLCJzdWIiOiJhbm9ueW1vdXMiLCJleHAiOjE3MzgyMzQyNzcsImlhdCI6MTczODIzMDY3NywianRpIjoiOWQ1ZTZmZGUtNmRjYi00YTExLWIwNGUtYzM1MWU0MTliNDkyIn0.YLMKy-anorv90rxtMbRrgZYCOM-bdu6UzBZqiCqYr9TTRetWGxb3IayVnJ1mwekBsx8UaZQNU7fQTc82mEtpOmtbSbxJCgY21hW4SWXXe8MkNCoZj4WMDaR8_lYxoU3LWAujjWfWF4cwUAR6hMDuc3J-xheKbI4bYCjodjXdYXmNXETUK8Q3_6q7wtla3av-cyfjxExaEPAmw5pjCBXxONVDe1YXxV8CHcVcQMuqFKoPXh-WL6aSEHHbNFR4NeLh3e0NKQRcbsyzeoujiASkDZDSkOXqJEJ6JRByDXyQetVEZ31n073BBMJf1Y6Dcp7mgnJ1o00R9xIhHBcDbtT2U0bYi8Zmjq2FQLrJjDKsZ9VwGcWDQs1b1DrfgkUNrvLSjOPQMCn2XZGhD50ikdMpVQbU_u6gY3_bK7TnXVornYpT_0YurPB4ttWkMEC-hGXKUnO7zmvp6ZqdirbLGpzQV-WV4MYA1hVx2QAt-C2hMwqByu3U9qOTP8ns2G_HFBLj",
  "tx-auth:refreshAudience": "did:web:bob",
  "@context": {
    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
    "edc": "https://w3id.org/edc/v0.0.1/ns/",
    "tx": "https://w3id.org/tractusx/v0.0.1/ns/",
    "tx-auth": "https://w3id.org/tractusx/auth/",
    "cx-policy": "https://w3id.org/catenax/policy/",
    "odrl": "http://www.w3.org/ns/odrl/2/"
  }
}
```

## Notice

This work is licensed under the [CC-BY-4.0](https://creativecommons.org/licenses/by/4.0/legalcode).

- SPDX-License-Identifier: CC-BY-4.0
- SPDX-FileCopyrightText: (c) 2025 Cofinity-X GmbH
- Source URL: [tractusx-edc-kafka-extension](https://github.com/eclipse-tractusx/tractusx-edc-kafka-extension)
