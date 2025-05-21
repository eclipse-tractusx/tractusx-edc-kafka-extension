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
package org.eclipse.tractusx.edc.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.*;

@Slf4j
public class KafkaConsumerApp {
    public static final String FORECAST_ASSET_ID = System.getenv().getOrDefault("FORECAST_ASSET_ID", "kafka-forecast-asset");
    public static final String TRACKING_ASSET_ID = System.getenv().getOrDefault("TRACKING_ASSET_ID", "kafka-tracking-asset");
    static final String ASSET_ID = System.getenv().getOrDefault("ASSET_ID", "kafka-stream-asset");
    static final String PROVIDER_ID = System.getenv().getOrDefault("PROVIDER_ID", "BPNL00000003AZQP");
    static final String PROVIDER_PROTOCOL_URL = System.getenv().getOrDefault("PROVIDER_PROTOCOL_URL", "http://control-plane-alice:8084/api/v1/dsp");
    static final String EDC_MANAGEMENT_URL = System.getenv().getOrDefault("EDC_MANAGEMENT_URL", "http://localhost:8081/management");
    static final String EDC_API_KEY = System.getenv().getOrDefault("EDC_API_KEY", "password");

    public static void main(final String[] args) {
        try {
            final List<EDRData> edrDataList = fetchAllEdrData();
            if (edrDataList.isEmpty()) {
                log.error("Failed to retrieve EDR data. Exiting application.");
                return;
            }

            runKafkaConsumer(edrDataList);
        } catch (final Exception e) {
            log.error("Fatal error in KafkaConsumerApp", e);
        }
    }

    private static List<EDRData> fetchAllEdrData() throws IOException, InterruptedException {
        final DataTransferClient client = new DataTransferClient();
        return List.of(
                client.executeDataTransferWorkflow(FORECAST_ASSET_ID),
                client.executeDataTransferWorkflow(TRACKING_ASSET_ID)
        );
    }

    private static void runKafkaConsumer(final List<EDRData> edrDataList) {
        try (final KafkaConsumer<String, String> consumer = initializeKafkaConsumer(edrDataList.getFirst())) {
            final List<String> topics = edrDataList.stream()
                    .map(EDRData::getTopic)
                    .filter(t -> t != null && !t.isBlank())
                    .toList();

            consumer.subscribe(topics);
            log.info("Consumer started with {} authentication. Waiting for messages...", edrDataList.getFirst().getKafkaSaslMechanism());
            while (true) {
                final ConsumerRecords<String, String> records = consumer.poll(Duration.parse(edrDataList.getFirst().getKafkaPollDuration()));
                for (final ConsumerRecord<String, String> consumerRecord : records) {
                    log.info("Received record(topic={} key={}, value={}) meta(partition={}, offset={})",consumerRecord.topic(), consumerRecord.key(), consumerRecord.value(), consumerRecord.partition(), consumerRecord.offset());
                }
            }
        }
    }

    private static KafkaConsumer<String, String> initializeKafkaConsumer(final EDRData edrData) {
        Objects.requireNonNull(edrData, "EDR data cannot be null");

        final Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, edrData.getKafkaBootstrapServers());
        props.put(GROUP_ID_CONFIG, edrData.getKafkaGroupPrefix());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "true"); // Automatically commit offsets
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest"); // Automatically reset the offset to the earliest offset
        props.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // Security settings from EDR Token (SASL/OAUTHBEARER)
        props.put(SECURITY_PROTOCOL_CONFIG, edrData.getKafkaSecurityProtocol());
        props.put(SASL_MECHANISM, edrData.getKafkaSaslMechanism());

        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS, EdrTokenCallbackHandler.class.getName());

        props.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");

        props.put(SASL_LOGIN_CONNECT_TIMEOUT_MS, "15000"); // (optional) timeout for external authentication provider connection in ms

        props.put(SASL_LOGIN_REFRESH_BUFFER_SECONDS, "120"); // Refresh 2 minutes before expiry
        props.put(SASL_LOGIN_REFRESH_MIN_PERIOD_SECONDS, "30"); // Don't refresh more than once per 30 seconds
        props.put(SASL_LOGIN_REFRESH_WINDOW_FACTOR, "0.8"); // Refresh at 80% of token lifetime
        props.put(SASL_LOGIN_REFRESH_WINDOW_JITTER, "0.05"); // Add small random jitter

        return new KafkaConsumer<>(props);
    }
}