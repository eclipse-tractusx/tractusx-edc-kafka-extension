/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.eclipse.tractusx.edc.kafka.consumer;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Objects;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.*;
import static org.apache.kafka.common.config.SslConfigs.*;
import static org.eclipse.tractusx.edc.kafka.consumer.KafkaConsumerApp.*;

/**
 * Factory for creating Kafka consumers.
 * This abstraction allows for better testability by enabling mocking of Kafka consumer creation.
 */
public class KafkaConsumerFactory {
    
    /**
     * Creates a new KafkaConsumer instance configured with the provided EDR data.
     * 
     * @param edrData the EDR data containing Kafka configuration
     * @return a configured KafkaConsumer instance
     * @throws IllegalArgumentException if edrData is invalid
     */
    public KafkaConsumer<String, String> createConsumer(EDRData edrData) {
        Objects.requireNonNull(edrData, "EDR data cannot be null");

        validateEdrData(edrData);

        Properties props = createKafkaProperties(edrData);
        return new KafkaConsumer<>(props);
    }

    private void validateEdrData(EDRData edrData) {
        if (edrData.getEndpoint() == null || edrData.getEndpoint().trim().isEmpty()) {
            throw new IllegalArgumentException("EDR data endpoint cannot be null or empty");
        }

        if (edrData.getKafkaGroupPrefix() == null || edrData.getKafkaGroupPrefix().trim().isEmpty()) {
            throw new IllegalArgumentException("EDR data group prefix cannot be null or empty");
        }

        if (edrData.getKafkaSecurityProtocol() == null || edrData.getKafkaSecurityProtocol().trim().isEmpty()) {
            throw new IllegalArgumentException("EDR data security protocol cannot be null or empty");
        }

        if (edrData.getKafkaSaslMechanism() == null || edrData.getKafkaSaslMechanism().trim().isEmpty()) {
            throw new IllegalArgumentException("EDR data SASL mechanism cannot be null or empty");
        }
    }

    private Properties createKafkaProperties(EDRData edrData) {
        Properties props = new Properties();

        // Basic Kafka consumer configuration
        props.put(BOOTSTRAP_SERVERS_CONFIG, edrData.getEndpoint());
        props.put(GROUP_ID_CONFIG, edrData.getKafkaGroupPrefix());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "true"); // Automatically commit offsets
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest"); // Automatically reset the offset to the earliest offset
        props.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Security settings from EDR Token (SASL/OAUTHBEARER)
        props.put(SECURITY_PROTOCOL_CONFIG, edrData.getKafkaSecurityProtocol());
        props.put(SASL_MECHANISM, edrData.getKafkaSaslMechanism());
        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS, EdrTokenCallbackHandler.class.getName());
        props.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");

        // Authentication refresh settings
        props.put(SASL_LOGIN_CONNECT_TIMEOUT_MS, "15000"); // (optional) timeout for external authentication provider connection in ms
        props.put(SASL_LOGIN_REFRESH_BUFFER_SECONDS, "120"); // Refresh 2 minutes before expiry
        props.put(SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS, "30"); // Don't refresh more than once per 30 seconds
        props.put(SASL_LOGIN_REFRESH_WINDOW_FACTOR, "0.8"); // Refresh at 80% of token lifetime
        props.put(SASL_LOGIN_REFRESH_WINDOW_JITTER, "0.05"); // Add small random jitter

        // SSL configuration for development with self-signed certificates
        props.put(SSL_TRUSTSTORE_LOCATION_CONFIG, KAFKA_SSL_TRUSTSTORE_LOCATION);
        props.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM);
        props.put(SSL_TRUSTSTORE_TYPE_CONFIG, KAFKA_SSL_TRUSTSTORE_TYPE);

        return props;
    }
}