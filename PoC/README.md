### Run the setup

### Build

To build the project use:

```bash
./gradlew build
```
 
### Run in Docker

#### Start docker compose

To start the `docker compose`, simply run configuration for IntelliJ `kafka-poc-docker` or
the following command from the project root:

```
docker compose --project-directory poc/runtimes up
```

#### Data transfer

In [bruno collection](kafka-pull/collections/Kafak%20PoC%20Bruno%20collection)
all HTTP requests required to run kafka pull transfers are prepared.
Using these requests, Alice will act as the provider and Bob will act as the consumer.

After initiating the Kafka pull transfer, the `transfer id` must be copied into the `TRANSFER_PROCESS_ID` property in
[KafkaConsumerApp.java](runtimes/kafka/kafka-consumer/src/main/java/org/eclipse/tractusx/edc/kafka/consumer/KafkaConsumerApp.java). 
Then, build the application and run it in Docker.

### DataAddress Schema

Information about `kafka-broker-extension` could be found in [README.md](kafka-pull/README.md)

