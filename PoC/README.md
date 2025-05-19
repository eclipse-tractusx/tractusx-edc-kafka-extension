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

```shell
docker compose --project-directory poc/runtimes up --build
```

#### Data transfer

In [bruno collection](kafka-pull/collections/Kafka%20PoC%20Bruno%20collection)
all HTTP requests required to run kafka pull transfers are prepared.
Using these requests, Alice will act as the provider and Bob will act as the consumer.

Run these requests on provider side:

- Create asset
- Create Contract definition
- Create policy

Then run these requests on consumer side:

- Get dataset
- Initiate Negotiation
- Get Negotiation
- Kafka PULL
- Get transfer process

When using the docker compose setup, the requests will be executed by the provider and consumer applications.
Afterward, data transfer will be initiated automatically.

### DataAddress Schema

Information about `kafka-broker-extension` could be found in [README.md](kafka-pull/README.md)

