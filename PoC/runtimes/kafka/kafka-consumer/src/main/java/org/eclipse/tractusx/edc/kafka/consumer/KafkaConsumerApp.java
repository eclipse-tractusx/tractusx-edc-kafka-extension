/*
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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

public class KafkaConsumerApp {
    private static final String TRANSFER_PROCESS_ID = "196e3b01-7a77-434a-91f2-f64305688ecd";
    public static void main(String[] args) {

        var edrProvider = new EdrProvider();
        EDRData edrData = edrProvider.getEdr(TRANSFER_PROCESS_ID);
        if(edrData == null)
            return;
        KafkaConsumer<String, String> consumer = initializeKafkaConsumer(edrData);

        consumer.subscribe(Collections.singletonList(edrData.getTopic()));
        System.out.println("Consumer started with SASL/SCRAM authentication. Waiting for messages...");

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));

                for (ConsumerRecord<String, String> record : records) {
                    System.out.printf("Received record(key=%s, value=%s) " +
                                    "meta(partition=%d, offset=%d)\n",
                            record.key(), record.value(), record.partition(), record.offset());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            consumer.close();
        }
    }

    private static KafkaConsumer<String, String> initializeKafkaConsumer(EDRData edrData) {
        Properties props = new Properties();
        props.put("bootstrap.servers", edrData.getEndpoint());
        props.put("group.id", edrData.getGroupPrefix());
        props.put("enable.auto.commit", "true"); // Automatically commit offsets
        props.put("auto.commit.interval.ms", "1000");
        props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
        props.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");

        // Security Configuration for SASL/SCRAM
        props.put("security.protocol", "SASL_PLAINTEXT"); // Use "SASL_SSL" if using SSL
        props.put("sasl.jaas.config",
                "org.apache.kafka.common.security.scram.ScramLoginModule required " +
                        "username=\"" + edrData.getAuthKey() + "\" " +
                        "password=\"" + edrData.getAuthCode() + "\" " +
                        "tokenauth=\"true\";");
        props.put("sasl.mechanism", "SCRAM-SHA-256"); // or "SCRAM-SHA-512" based on your Kafka setup


        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        return consumer;
    }
}