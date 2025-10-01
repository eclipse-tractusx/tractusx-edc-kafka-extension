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

package org.eclipse.tractusx.edc.kafka.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.eclipse.tractusx.edc.kafka.producer.config.ProducerProperties;
import org.eclipse.tractusx.edc.kafka.producer.messages.ForecastMessage;
import org.eclipse.tractusx.edc.kafka.producer.messages.Message;
import org.eclipse.tractusx.edc.kafka.producer.messages.MessageLoader;
import org.eclipse.tractusx.edc.kafka.producer.messages.TrackingMessage;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class KafkaProducerService {
    private final KafkaProducer<String, String> producer;
    private final MessageLoader messageLoader;
    private final ObjectMapper objectMapper;
    private final ProducerProperties config;

    public KafkaProducerService(KafkaProducer<String, String> producer, MessageLoader messageLoader, ObjectMapper objectMapper, ProducerProperties config) {
        this.producer = producer;
        this.messageLoader = messageLoader;
        this.objectMapper = objectMapper;
        this.config = config;
    }

    public void runProducer() throws InterruptedException, IOException {
        int forecastIndex = 0;
        int trackingIndex = 0;
        int counter = 0;
        final List<ForecastMessage> forecasts = messageLoader.loadForecastMessages();
        final List<TrackingMessage> trackings = messageLoader.loadTrackingMessages();

        while (!Thread.currentThread().isInterrupted()) {
            final boolean sendForecast = (counter++ % 2 == 0);

            if (sendForecast) {
                final ForecastMessage msg = forecasts.get(forecastIndex++ % forecasts.size());
                sendMessage(msg, config.getProductionForecastTopic());
            } else {
                final TrackingMessage msg = trackings.get(trackingIndex++ % trackings.size());
                sendMessage(msg, config.getProductionTrackingTopic());
            }
            TimeUnit.MILLISECONDS.sleep(config.getMessageSendIntervalMs());
        }
    }

    public void sendMessage(final Message message, final String topic) throws IOException {
        final String payload = objectMapper.writeValueAsString(message);
        final String key = extractKey(message);
        sendMessage(topic, key, payload).whenComplete((sendAck, e) -> {
            if (e == null) {
                log.info("Sent record(topic={} key={} message={}) meta(partition={}, offset={})", sendAck.producerRecord().topic(), sendAck.producerRecord().key(), sendAck.producerRecord().value(), sendAck.metadata().partition(), sendAck.metadata().offset());
            } else {
                log.error("Failed to send record: {}", e.getMessage(), e);
            }
        });
    }

    public CompletableFuture<SendAck<String, String>> sendMessage(String topic, String key, String message) {
        final ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topic, key, message);
        CompletableFuture<SendAck<String, String>> future = new CompletableFuture<>();
        producer.send(producerRecord, (final RecordMetadata metadata, final Exception e) -> {
            if (e == null) {
                future.complete(new SendAck<>(producerRecord, metadata));
            } else {
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    private String extractKey(final Object message) {
        return message.getClass().getSimpleName() + "-" + System.currentTimeMillis();
    }
}