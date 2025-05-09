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

import java.time.Duration;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;

import static org.apache.kafka.clients.CommonClientConfigs.SECURITY_PROTOCOL_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.common.config.SaslConfigs.*;

@Slf4j
public class KafkaConsumerApp {

    public static void main(final String[] args) {
        try {
            EDRData edrData = fetchEdrData();
            if (edrData == null) {
                log.error("Failed to retrieve EDR data. Exiting application.");
                return;
            }

            runKafkaConsumer(edrData);
        } catch (Exception e) {
            log.error("Fatal error in KafkaConsumerApp", e);
        }
    }

    private static EDRData fetchEdrData() {
        log.info("Fetching EDR data...");
        EdrProvider edrProvider = new EdrProvider();
        return edrProvider.getEdr();
    }

    private static void runKafkaConsumer(final EDRData edrData) {
        try (KafkaConsumer<String, String> consumer = initializeKafkaConsumer(edrData)) {
            String topic = edrData.getTopic();
            if (topic == null || topic.isBlank()) {
                throw new IllegalArgumentException("Topic cannot be null or empty");
            }
            consumer.subscribe(Collections.singletonList(topic));
            log.info("Consumer started with {} authentication. Waiting for messages...", edrData.getKafkaSaslMechanism());
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("Received record(key={}, value={}) meta(partition={}, offset={})", record.key(), record.value(), record.partition(), record.offset());
                }
            }
        } catch (Exception e) {
            log.error("Error while consuming Kafka messages", e);
        }
    }

    private static KafkaConsumer<String, String> initializeKafkaConsumer(final EDRData edrData) {
        Objects.requireNonNull(edrData, "EDR data cannot be null");

        Properties props = new Properties();
//        props.put(BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(BOOTSTRAP_SERVERS_CONFIG, edrData.getEndpoint());
        props.put(GROUP_ID_CONFIG, edrData.getGroupPrefix());
        props.put(ENABLE_AUTO_COMMIT_CONFIG, "true"); // Automatically commit offsets
        props.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        // Security settings from EDR Token (SASL/OAUTHBEARER)
        props.put(SECURITY_PROTOCOL_CONFIG, edrData.getKafkaSecurityProtocol());
        props.put(SASL_MECHANISM, edrData.getKafkaSaslMechanism());

        props.put(SASL_LOGIN_CALLBACK_HANDLER_CLASS, "org.eclipse.tractusx.edc.kafka.consumer.EdrTokenCallbackHandler");

        props.put(SASL_JAAS_CONFIG, "org.apache.kafka.common.security.oauthbearer.OAuthBearerLoginModule required;");

        props.put(SASL_LOGIN_CONNECT_TIMEOUT_MS, "15000"); // optional

        return new KafkaConsumer<>(props);
    }
}