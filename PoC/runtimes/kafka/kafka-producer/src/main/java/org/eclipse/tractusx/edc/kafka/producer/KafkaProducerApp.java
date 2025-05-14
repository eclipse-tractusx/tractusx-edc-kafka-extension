/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 Cofinity-X GmbH
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
package org.eclipse.tractusx.edc.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.net.http.HttpClient;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.*;

@Slf4j
public class KafkaProducerApp {
    static final String KAFKA_STREAM_TOPIC = System.getenv().getOrDefault("KAFKA_STREAM_TOPIC", "kafka-stream-topic");
    static final String KEYCLOAK_CLIENT_ID = System.getenv().getOrDefault("KEYCLOAK_CLIENT_ID", "default");
    static final String KEYCLOAK_CLIENT_SECRET = System.getenv().getOrDefault("KEYCLOAK_CLIENT_SECRET", "mysecret");
    static final String VAULT_CLIENT_SECRET_KEY = System.getenv().getOrDefault("VAULT_CLIENT_SECRET_KEY", "secretKey");
    static final String KEYCLOAK_TOKEN_URL = System.getenv().getOrDefault("KEYCLOAK_TOKEN_URL", "http://localhost:8080/realms/kafka/protocol/openid-connect/token");
    static final String KEYCLOAK_REVOKE_URL = System.getenv().getOrDefault("KEYCLOAK_REVOKE_URL", "http://localhost:8080/realms/kafka/protocol/openid-connect/revoke");
    static final String KAFKA_BOOTSTRAP_SERVERS = System.getenv().getOrDefault("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092");
    static final String ASSET_ID = System.getenv().getOrDefault("ASSET_ID", "kafka-stream-asset");
    static final String EDC_API_AUTH_KEY = System.getenv().getOrDefault("EDC_API_AUTH_KEY", "password");
    static final String EDC_MANAGEMENT_URL = System.getenv().getOrDefault("EDC_MANAGEMENT_URL", "http://localhost:8081/management");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final long MESSAGE_INTERVAL_MS = 1000;

    public static void main(final String[] args) {
        try (HttpClient client = HttpClient.newHttpClient()) {
            EdcSetup edcSetup = new EdcSetup(client);
            edcSetup.setupEdcOffer();
        }

        try (KafkaProducer<String, String> producer = createProducer()) {
            log.info("Starting producer...");
            runProducer(producer);
        } catch (InterruptedException e) {
            log.info("Producer interrupted: {}", e.getMessage());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Error in producer: {}", e.getMessage(), e);
        }
    }

    private static void runProducer(final KafkaProducer<String, String> producer) throws InterruptedException {
        while (true) {
            try {
                Data data = generateRandomData();
                String jsonPayload = serializeData(data);
                sendMessage(producer, data.id(), jsonPayload);
                TimeUnit.MILLISECONDS.sleep(MESSAGE_INTERVAL_MS);
            } catch (JsonProcessingException e) {
                log.error("Error serializing data: {}", e.getMessage(), e);
            }
        }
    }

    private static void sendMessage(final KafkaProducer<String, String> producer, final String key, final String value) {
        ProducerRecord<String, String> record = new ProducerRecord<>(KAFKA_STREAM_TOPIC, key, value);
        producer.send(record, (final RecordMetadata metadata, final Exception e) -> {
            if (e != null) {
                log.error("Failed to send record: {}", e.getMessage(), e);
            } else {
                log.info("Sent record(key={} value={}) meta(partition={}, offset={})",
                        record.key(), record.value(), metadata.partition(), metadata.offset());
            }
        });
    }

    private static String serializeData(final Data data) throws JsonProcessingException {
        return OBJECT_MAPPER.writeValueAsString(data);
    }

    private static Data generateRandomData() {
        return new Data(
                UUID.randomUUID().toString(),
                Math.round(Math.random() * 100),
                Math.round(Math.random() * 100),
                Math.round(Math.random() * 100)
        );
    }

    static KafkaProducer<String, String> createProducer() {
        Properties props = new Properties();

        // Basic producer settings
        props.put(BOOTSTRAP_SERVERS_CONFIG, KAFKA_BOOTSTRAP_SERVERS);
        props.put(ACKS_CONFIG, "all");
        props.put(RETRIES_CONFIG, 0);
        props.put(BATCH_SIZE_CONFIG, 16384); // 16KB
        props.put(LINGER_MS_CONFIG, 1);
        props.put(BUFFER_MEMORY_CONFIG, 33554432); // 32MB
        props.put(DELIVERY_TIMEOUT_MS_CONFIG, 3000);
        props.put(REQUEST_TIMEOUT_MS_CONFIG, 2000);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        // Security settings for SASL/OAUTHBEARER
        props.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        props.put(SASL_MECHANISM, "OAUTHBEARER");

        // OAuth properties
        props.put(SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, KEYCLOAK_TOKEN_URL);
        props.put("sasl.oauthbearer.client.id", KEYCLOAK_CLIENT_ID);
        props.put("sasl.oauthbearer.client.secret", KEYCLOAK_CLIENT_SECRET);
        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS,
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");

        // JAAS configuration for OAuth2
        props.put(SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                        "clientId=\"" + KEYCLOAK_CLIENT_ID + "\" " +
                        "clientSecret=\"" + KEYCLOAK_CLIENT_SECRET + "\";"
        );

        return new KafkaProducer<>(props);
    }
}