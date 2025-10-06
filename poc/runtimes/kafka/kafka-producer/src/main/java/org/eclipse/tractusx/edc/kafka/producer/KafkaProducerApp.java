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

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.edc.kafka.producer.config.KafkaConfig;
import org.eclipse.tractusx.edc.kafka.producer.config.ProducerProperties;

import java.io.IOException;

import static org.eclipse.tractusx.edc.kafka.producer.config.KafkaConfig.*;

@Slf4j
public class KafkaProducerApp {
    static final String KAFKA_SSL_TRUSTSTORE_LOCATION = System.getenv().getOrDefault("SSL_TRUSTSTORE_LOCATION", "/opt/java/openjdk/lib/security/cacerts");
    static final String KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM = System.getenv().getOrDefault("SSL_ENDPOINT_IDENTIFICATION_ALGORITHM", "");
    static final String KAFKA_SSL_TRUSTSTORE_TYPE = System.getenv().getOrDefault("SSL_TRUSTSTORE_TYPE", "JKS");
    public static final String KAFKA_SECURITY_PROTOCOL = System.getenv().getOrDefault("SECURITY_PROTOCOL", "SASL_PLAINTEXT");

    public static void main(final String[] args) throws IOException, InterruptedException {
        log.info("Starting Kafka producer application...");

        ProducerProperties config = new ProducerProperties();

        KafkaConfig kafkaConfigService = new KafkaConfig(config);
        var edcSetup = new EdcSetup(httpClient(), config);
        edcSetup.setupEdcOffer();

        var kafkaProducerAppService = new KafkaProducerService(
            kafkaConfigService.createKafkaProducer(),
            messageLoader(objectMapper()),
            objectMapper(),
            config
        );

        log.info("Starting producer...");
        kafkaProducerAppService.runProducer();
    }
//        // Security settings for SASL/OAUTHBEARER
//        props.put(SECURITY_PROTOCOL_CONFIG, KAFKA_SECURITY_PROTOCOL);
//        props.put(SASL_MECHANISM, "OAUTHBEARER");
//
//        // OAuth properties
//        props.put(SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, KEYCLOAK_TOKEN_URL);
//        props.put("sasl.oauthbearer.client.id", KEYCLOAK_CLIENT_ID);
//        props.put("sasl.oauthbearer.client.secret", KEYCLOAK_CLIENT_SECRET);
//        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS,
//                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");
//
//        // JAAS configuration for OAuth2
//        props.put(SASL_JAAS_CONFIG,
//                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
//                        "clientId=\"" + KEYCLOAK_CLIENT_ID + "\" " +
//                        "clientSecret=\"" + KEYCLOAK_CLIENT_SECRET + "\";"
//        );
//
//        // SSL configuration for development with self-signed certificates
//        props.put(SSL_TRUSTSTORE_LOCATION_CONFIG, KAFKA_SSL_TRUSTSTORE_LOCATION);
//        props.put(SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, KAFKA_SSL_ENDPOINT_IDENTIFICATION_ALGORITHM);
//        props.put(SSL_TRUSTSTORE_TYPE_CONFIG, KAFKA_SSL_TRUSTSTORE_TYPE);
    static KafkaProducer<String, String> createProducer() {
        final Properties props = new Properties();

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