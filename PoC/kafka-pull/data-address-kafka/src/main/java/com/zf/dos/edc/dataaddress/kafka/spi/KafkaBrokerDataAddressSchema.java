package com.zf.dos.edc.dataaddress.kafka.spi;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

/**
 * Defines the schema of a DataAddress representing a Kafka endpoint.
 */
public interface KafkaBrokerDataAddressSchema {

    /**
     * The transfer type.
     */
    String KAFKA_TYPE = "KafkaBroker";

    /**
     * The Kafka topic that will be allowed to poll for the consumer.
     */
    String TOPIC = EDC_NAMESPACE + "topic";

    /**
     * The bootstrap.servers property
     */
    String BOOTSTRAP_SERVERS = "kafka.bootstrap.servers";

    /**
     * The sasl.mechanism property
     */
    String MECHANISM = "kafka.sasl.mechanism";

    /**
     * The security.protocol property
     */
    String PROTOCOL = "kafka.security.protocol";

    /**
     * The duration of the consumer polling.
     * <p>
     * The value should be a ISO-8601 duration e.g. "PT10S" for 10 seconds.
     * This parameter is optional. Default value is 1s.
     *
     * @see java.time.Duration#parse(CharSequence) for ISO-8601 duration format
     */
    String POLL_DURATION = EDC_NAMESPACE + "pollDuration";

    /**
     * The secret token/credentials
     */
    String SECRET_KEY = EDC_NAMESPACE + "secretKey";

    /**
     * The groupPrefix that will be allowed to use for the consumer
     */
    String GROUP_PREFIX = EDC_NAMESPACE + "groupPrefix";
}
