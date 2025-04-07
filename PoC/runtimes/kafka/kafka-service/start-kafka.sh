#!/bin/bash
set -ex

# Export environment variables from kafka.env
export $(grep -v '^#' /config/temp_kafka.env | xargs)

export KAFKA_OPTS="-Djava.security.auth.login.config=/config/temp_kafka_broker_jaas.conf"

# Define the data directory
DATA_DIR="/var/lib/kafka/data"

# Check if meta.properties exists
if [ ! -f "$DATA_DIR/meta.properties" ]; then
  echo "Formatting storage directories..."

  # Generate a cluster ID if not already set
  if [ -z "$CLUSTER_ID" ]; then
    CLUSTER_ID=$(/usr/bin/kafka-storage random-uuid)
    echo "Generated Cluster ID: $CLUSTER_ID"
  fi

  # Format the storage directories
  /usr/bin/kafka-storage format -t $CLUSTER_ID -c /config/temp_server.properties
else
  echo "Storage directories already formatted."
fi

if [ -f /config/temp_kafka.env ]; then
    echo "Loading environment variables from temp_kafka.env"
    set -a
    source /config/temp_kafka.env
    set +a
else
    echo "Error: temp_kafka.env file not found!"
    exit 1
fi

echo "Starting Kafka in KRaft mode with PLAIN authentication..."
/usr/bin/kafka-server-start /config/temp_server.properties &

# Wait for Kafka to initialize
sleep 5

echo "Temp Kafka has started. Creating SCRAM credentials..."
# Create SCRAM credentials for the 'admin' user
/usr/bin/kafka-configs --bootstrap-server localhost:9092 \
  --entity-type users --entity-name admin \
  --alter --add-config 'SCRAM-SHA-256=[password=admin-secret]' \
  --command-config /config/admin-client.properties

echo "SCRAM credentials for 'admin' created."

pkill -f kafka

echo "Starting Kafka in KRaft mode with SASL/SCRAM authentication..."

# Export environment variables from kafka.env
export $(grep -v '^#' /config/kafka.env | xargs)

# Set the JAAS configuration file for both broker and controller
export KAFKA_OPTS="-Djava.security.auth.login.config=/config/kafka_broker_jaas.conf"
export KAFKA_CONTROLLER_OPTS="-Djava.security.auth.login.config=/config/kafka_broker_jaas.conf"

if [ -f /config/kafka.env ]; then
    echo "Loading environment variables from kafka.env"
    set -a
    source /config/kafka.env
    set +a
else
    echo "Error: kafka.env file not found!"
    exit 1
fi

# Start Kafka in the background
/usr/bin/kafka-server-start /config/server.properties &

# Wait for Kafka to start
while ! (echo > /dev/tcp/localhost/9092) &>/dev/null; do
  sleep 1
done

echo "Kafka has started. Creating topic: kafka-stream-topic"

# Create the topic
/usr/bin/kafka-topics.sh --create \
    --bootstrap-server localhost:9092 \
    --replication-factor 1 \
    --partitions 1 \
    --topic kafka-stream-topic || echo "Topic kafka-stream-topic already exists."

# Bring Kafka to the foreground
wait
