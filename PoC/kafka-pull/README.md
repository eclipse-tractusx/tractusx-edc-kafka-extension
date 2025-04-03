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

Please see [bruno collection](collections/Kafak%20PoC%20Bruno%20collection).

## How to run

### build the connector JAR

````shell
./gradlew :kafka-broker-extension:build
````

### Run the connector JAR

#### As provider

````shell
export EDC_FS_CONFIG="PoC/kafka-pull/kafka-broker-extension/provider.properties"
java -jar PoC/kafka-pull/kafka-broker-extension/build/libs/connector.jar
````

#### As consumer

````shell
export EDC_FS_CONFIG="PoC/kafka-pull/kafka-broker-extension/consumer.properties"
java -jar PoC/kafka-pull/kafka-broker-extension/build/libs/connector.jar
````

### Start Kafka and configure ACLs

Kafka will be started in [KRaft mode](https://developer.confluent.io/learn/kraft/), a single broker with
`SASL_PLAINTEXT` as security protocol ([see config](kafka-config/kafka.env)), there will be an `admin` user, responsible
for setting up ACLs and producing messages, and `alice`, that will be used by the consumer to consume the messages.

Run the Kafka container:

````shell
docker run --rm --name=kafka-kraft -h kafka-kraft -p 9093:9093 \
    -v "$PWD/transfer/streaming/streaming-03-kafka-broker/kafka-config":/config \
    --env-file transfer/streaming/streaming-03-kafka-broker/kafka-config/kafka.env \
    -e KAFKA_NODE_ID=1 \
    -e KAFKA_LISTENERS='PLAINTEXT://0.0.0.0:9093,BROKER://0.0.0.0:9092,CONTROLLER://0.0.0.0:9094' \
    -e KAFKA_ADVERTISED_LISTENERS='PLAINTEXT://localhost:9093,BROKER://localhost:9092' \
    -e KAFKA_PROCESS_ROLES='broker,controller' \
    -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 \
    -e KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR=1 \
    -e KAFKA_CONTROLLER_QUORUM_VOTERS='1@localhost:9094' \
    -e KAFKA_INTER_BROKER_LISTENER_NAME='BROKER' \
    -e KAFKA_CONTROLLER_LISTENER_NAMES='CONTROLLER' \
    -e KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS=1 \
    -e CLUSTER_ID='4L6g3nShT-eMCtK--X86sw' \
    confluentinc/cp-kafka:7.5.2
````

Create the topic `kafka-stream-topic`

````shell
docker exec -it kafka-kraft /bin/kafka-topics \
  --topic kafka-stream-topic --create --partitions 1 --replication-factor 1 \
  --command-config=/config/admin.properties \
  --bootstrap-server localhost:9092
````

To give `alice` read permissions on the topic we need to set up ACLs:

````shell
docker exec -it kafka-kraft /bin/kafka-acls --command-config /config/admin.properties \
  --bootstrap-server localhost:9093 \
  --add --allow-principal 'User:alice' \
  --topic kafka-stream-topic \
  --group group_id \
  --operation Read
````
