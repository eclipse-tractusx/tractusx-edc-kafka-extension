# KafkaBroker Control Plane module

## About this module

This module contains a Control Plane extension(`kafka-broker-extension`) that allow to manage access for specific topic by creating SCRAM credentials and token directly in provider's kafka.

### Concept

In this sample the dataplane is not used, the consumer will set up a kafka client to poll the messages from the broker using credentials obtained from the transfer process.

The DataFlow is managed by the KafkaBrokerDataFlowController, that on flow initialization creates the credentials for the consumer with access to poll data only from specific topic. Base on these credentials generates token. Finally, it provides an EDR with all necessary information for the consumer to poll the messages.

### DataAddress Schema

#### Properties

| Key                       | Description                                                                                                         | Mandatory |
|:--------------------------|:--------------------------------------------------------------------------------------------------------------------|-----------|
| `type`                    | Identifier of kafka data address. Should be set as 'KafkaBroker'                                                    | `true`    |
| `topic`                   | Defines the Kafka topic                                                                                             | `true`    |
| `kafka.bootstrap.servers` | Defines a custom endpoint URL to kafka                                                                              | `true`    |
| `kafka.sasl.mechanism`    | Defines the SASL kafka mechanism (SCRAM-SHA-256 or SCRAM-SHA-512)                                                   | `true`    |
| `kafka.security.protocol` | Defines the kafka  security protocol (SASL_PLAINTEXT or SASL_SSL)                                                   | `true`    |
| `pollDuration`            | Defines the duration of the consumer polling. The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.  | `false`   |
| `secretKey`               | Defines the `vault` entry containing the secret token/credentials                                                   | `true`    |
| `groupPrefix`             | Defines the groupPrefix that will be allowed to use for the consumer                                                | `true`    |

#### Secret Resolution

The `secretKey` property should point to a `vault` entry that contains a string object.

  ```
  org.apache.kafka.common.security.scram.ScramLoginModule required username="admin" password="admin-secret";
  ```
It should be admin credential that has permission to create additional credentials with permissions to read topic.

#### Examples:

Asset Data Address:
```json
{
  "dataAddress": {
    "type": "KafkaBroker",
    "bootstrap.servers": "localhost:9093",
    "topic": "kafka-stream-topic",
    "pollDuration": "PT5M",
    "secretKey": "secretKey",
    "sasl.mechanism": "SCRAM-SHA-256",
    "security.protocol":"SASL_PLAINTEXT"
  }
}
```

EDR:
```json
{
  "@type": "DataAddress",
  "kafka.security.protocol": "SASL_PLAINTEXT",
  "kafka.sasl.mechanism": "SCRAM-SHA-256",
  "groupPrefix": "bob",
  "type": "EDR",
  "endpoint": "localhost:9093",
  "authCode": "fB5UUYQXLU3wwJtCGUvmYVvySxx7vD/5J7DXlnnmfrx+m5UhpMaIhsP1aEaSGhXEJY9Kk8J+O6UW0GIt7TluaA==",
  "contractId": "dd4796b1-3232-4156-9921-94afb0abd537",
  "topic": "kafka-stream-topic",
  "id": "acba6ff2-4502-46af-a3a4-10378b663b71",
  "authKey": "lQTPm6RBQ6G7n5tXmOxQbg",
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
