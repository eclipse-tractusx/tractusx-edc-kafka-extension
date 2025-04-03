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

package org.eclipse.tractusx.edc.kafka.producer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import java.util.Properties;
import java.util.UUID;

public class KafkaProducerApp {
    private static final String BOOTSTRAP_SERVERS = "kafka-kraft:9093";
    private static final String TOPIC = "kafka-stream-topic";

    public static void main(String[] args) {
        KafkaProducer<String, String> producer = initializeKafkaProducer();
        ObjectMapper mapper = new ObjectMapper();

        try {
            while (true) {
                Data data = getData();
                String jsonString = mapper.writeValueAsString(data);
                ProducerRecord<String, String> record = new ProducerRecord<>(TOPIC, data.getId(), jsonString);

                producer.send(record, (RecordMetadata metadata, Exception e) -> {
                    if (e != null) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    } else {
                        System.out.printf("Sent record(key=%s value=%s) " +
                                        "meta(partition=%d, offset=%d)\n",
                                record.key(), record.value(), metadata.partition(), metadata.offset());
                    }
                });

                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        } finally {
            producer.close();
        }
    }

    private static KafkaProducer<String, String> initializeKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", BOOTSTRAP_SERVERS);
        props.put("acks", "all"); // Ensure all replicas acknowledge
        props.put("retries", 0);
        props.put("batch.size", 16384); // 16KB
        props.put("linger.ms", 1);
        props.put("buffer.memory", 33554432); // 32MB
        props.put("delivery.timeout.ms", 3000);
        props.put("request.timeout.ms", 2000);
        props.put("security.protocol", "SASL_PLAINTEXT"); // Use "SASL_SSL" if using SSL
        props.put("sasl.mechanism", "SCRAM-SHA-256"); // or
        props.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"admin\" password=\"admin-secret\";");
        props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        KafkaProducer<String, String> producer = new KafkaProducer<>(props);
        return producer;
    }

    private static Data getData() {
        Data data = new Data(
                UUID.randomUUID().toString(),
                Math.round((Math.random() * 100)),
                Math.round(Math.random() * 100),
                Math.round(Math.random() * 100)
        );
        return data;
    }
}