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

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service responsible for consuming messages from Kafka topics.
 * This class provides testable topic consumption functionality with controllable lifecycle.
 */
@Slf4j
@Service
public class KafkaTopicConsumptionService {

    private final KafkaConsumerFactory consumerFactory;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final MessageHandler messageHandler;

    @Autowired
    public KafkaTopicConsumptionService(KafkaConsumerFactory consumerFactory, MessageHandler messageHandler) {
        this.consumerFactory = consumerFactory;
        this.messageHandler = messageHandler;
    }

    /**
     * Starts consuming messages from the specified topics.
     * This method is controllable and can be stopped by calling stop().
     */
    public void startConsumption(List<EDRData> edrDataList) {
        if (edrDataList == null || edrDataList.isEmpty()) {
            throw new IllegalArgumentException("EDR data list cannot be null or empty");
        }

        running.set(true);
        EDRData primaryEdrData = edrDataList.getFirst();

        try (KafkaConsumer<String, String> consumer = consumerFactory.createConsumer(primaryEdrData)) {
            List<String> topics = extractValidTopics(edrDataList);
            if (topics.isEmpty()) {
                log.warn("No valid topics found in EDR data list");
                return;
            }

            consumer.subscribe(topics);
            log.info("Consumer started with {} authentication. Waiting for messages...",
                    primaryEdrData.getKafkaSaslMechanism());

            while (running.get()) {
                consumeMessages(primaryEdrData, consumer);
            }
        } catch (Exception e) {
            throw new KafkaConsumerException("Failed to consume topics", e);
        } finally {
            running.set(false);
            log.info("Kafka topic consumption stopped");
        }
    }

    private void consumeMessages(EDRData primaryEdrData, KafkaConsumer<String, String> consumer) {
        try {
            Duration pollDuration = parsePollDuration(primaryEdrData.getKafkaPollDuration());
            ConsumerRecords<String, String> records = consumer.poll(pollDuration);

            for (ConsumerRecord<String, String> consumerRecord : records) {
                if (!running.get()) {
                    break;
                }
                messageHandler.handleMessage(consumerRecord);
            }
        } catch (Exception e) {
            if (running.get()) {
                log.error("Error during message consumption", e);
            }
        }
    }

    public ConsumerRecords<String, String> consumeOnce(final EDRData edrData) {
        if (edrData == null) {
            throw new IllegalArgumentException("EDR data list cannot be null");
        }

        try (KafkaConsumer<String, String> consumer = consumerFactory.createConsumer(edrData)) {
            List<String> topics = extractValidTopics(List.of(edrData));
            if (topics.isEmpty()) {
                throw new KafkaConsumerException("No valid topics found in EDR data list");
            }
            consumer.subscribe(topics);
            log.info("Consumer subscribed with {} authentication. Polling for messages...",
                    edrData.getKafkaSaslMechanism());
            Duration pollDuration = parsePollDuration(edrData.getKafkaPollDuration());

            return consumer.poll(pollDuration);
        }
    }

    /**
     * Stops the consumption loop gracefully.
     */
    public void stop() {
        log.info("Stopping Kafka topic consumption...");
        running.set(false);
    }

    /**
     * Checks if the consumption service is currently running.
     */
    public boolean isRunning() {
        return running.get();
    }

    private List<String> extractValidTopics(List<EDRData> edrDataList) {
        return edrDataList.stream()
                .map(EDRData::getTopic)
                .filter(topic -> topic != null && !topic.isBlank())
                .toList();
    }

    private Duration parsePollDuration(String pollDurationStr) {
        try {
            return Duration.parse(pollDurationStr);
        } catch (Exception e) {
            log.warn("Invalid poll duration '{}', using default PT10S", pollDurationStr);
            return Duration.parse("PT10S");
        }
    }

}