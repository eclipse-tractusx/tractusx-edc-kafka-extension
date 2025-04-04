FROM confluentinc/cp-kafka:7.7.2

# Switch to root to perform privileged actions
USER root

# Copy configuration files into the container
COPY kafka.env /config/
COPY temp/temp_kafka.env /config/
COPY kafka_broker_jaas.conf /config/
COPY temp/temp_kafka_broker_jaas.conf /config/
COPY temp/admin-client.properties /config/
COPY admin.properties /config/
COPY server.properties /config/
COPY temp/temp_server.properties /config/
COPY start-kafka.sh /usr/bin/
RUN ls -l /usr/bin

# Ensure the script has execute permissions
RUN chmod +x /usr/bin/start-kafka.sh

# Switch back to the default user
USER 1001

# Set the entrypoint to your custom startup script
ENTRYPOINT ["/usr/bin/start-kafka.sh"]
