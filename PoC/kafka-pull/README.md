# KafkaBroker Control Plane module

## About this module

This module contains a Control Plane extension(`kafka-broker-extension`) that allow to manage access for specific topic 
by creating SCRAM credentials and token directly in provider's kafka.

### Concept

In this sample the dataplane is not used, the consumer will set up a kafka client to poll the messages from the broker 
using credentials obtained from the transfer process.

The DataFlow is managed by the KafkaBrokerDataFlowController, that on flow initialization creates the credentials for 
the consumer with access to poll data only from specific topic. Base on these credentials generates token. Finally, it 
provides an EDR with all necessary information for the consumer to poll the messages.


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

The `secretKey` property should point to a `vault` entry that contains a string object with admin kafka credentials.

  ```
secretKey=org.apache.kafka.common.security.scram.ScramLoginModule required username="admin" password="admin-secret";
  ```
It should be admin credential that has permission to create additional credentials with permissions to read topic.

#### Examples:

Please see postman [collection]((postman/Kafak%20PoC%20collection.postman_collection.json)).

