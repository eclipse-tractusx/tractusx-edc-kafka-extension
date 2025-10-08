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
package org.eclipse.tractusx.edc.kafka.producer.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.eclipse.tractusx.edc.kafka.producer.messages.MessageLoader;
import org.eclipse.tractusx.edc.kafka.producer.messages.ResourceMessageLoader;

import java.net.http.HttpClient;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.*;
/**
 * Configuration for Kafka producer setup.
 */
public class KafkaConfig {
    private final ProducerProperties config;

    public KafkaConfig(ProducerProperties config) {
        this.config = config;
    }

    public Properties kafkaProperties() {
        Properties configProps = new Properties();

        // Basic producer settings
        configProps.put(BOOTSTRAP_SERVERS_CONFIG, config.getBootstrapServers());
        configProps.put(RETRIES_CONFIG, 0);
        configProps.put(DELIVERY_TIMEOUT_MS_CONFIG, 3000);
        configProps.put(REQUEST_TIMEOUT_MS_CONFIG, 3000);
        configProps.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        // Security settings for SASL/OAUTHBEARER
        configProps.put(SECURITY_PROTOCOL_CONFIG, "SASL_PLAINTEXT");
        configProps.put(SASL_MECHANISM, "OAUTHBEARER");

        // OAuth properties
        configProps.put(SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL, config.getTokenUrl());

        configProps.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS,
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginCallbackHandler");

        // JAAS configuration for OAuth2
        configProps.put(SASL_JAAS_CONFIG,
                "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required " +
                        "clientId=\"" + config.getClientId() + "\" " +
                        "clientSecret=\"" + config.getClientSecret() + "\";");
        return configProps;
    }

    public static ObjectMapper objectMapper() {
        return new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public static MessageLoader messageLoader(final ObjectMapper objectMapper) {
        return new ResourceMessageLoader(objectMapper);
    }

    public static HttpClient httpClient() {
        return HttpClient.newHttpClient();
    }

    public KafkaProducer<String, String> createKafkaProducer() {
        return kafkaProducer(kafkaProperties());
    }

    public static KafkaProducer<String, String> kafkaProducer(Properties kafkaProperties) {
        return new KafkaProducer<>(kafkaProperties);
    }
}