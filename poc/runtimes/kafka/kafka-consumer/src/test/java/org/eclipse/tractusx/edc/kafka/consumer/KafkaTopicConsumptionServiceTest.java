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

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaTopicConsumptionServiceTest {

    @Mock
    private KafkaConsumerFactory consumerFactory;

    @Mock
    private KafkaConsumer<String, String> kafkaConsumer;

    @Mock
    private Consumer<ConsumerRecord<String, String>> messageHandler;

    private KafkaTopicConsumptionService consumptionService;

    @BeforeEach
    void setUp() {
        consumptionService = new KafkaTopicConsumptionService(consumerFactory, messageHandler);
    }

    @ParameterizedTest
    @MethodSource("invalidEdrDataListProvider")
    void shouldThrowExceptionForInvalidEdrDataList(List<EDRData> edrDataList) {
        // Act & Assert
        assertThatThrownBy(() -> consumptionService.startConsumption(edrDataList))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("EDR data list cannot be null or empty");
    }

    static Stream<Arguments> invalidEdrDataListProvider() {
        return Stream.of(
                Arguments.of((List<EDRData>) null),
                Arguments.of(Collections.emptyList())
        );
    }

    @Test
    void shouldSubscribeToTopicsAndConsumeMessages() throws Exception {
        // Arrange
        when(consumerFactory.createConsumer(any())).thenReturn(kafkaConsumer);
        EDRData edrData1 = createEdrData("topic1");
        EDRData edrData2 = createEdrData("topic2");
        List<EDRData> edrDataList = List.of(edrData1, edrData2);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("topic1", 0, 0L, "key1", "value1");
        ConsumerRecords<String, String> records = createConsumerRecords(record);

        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(records, new ConsumerRecords<>(Collections.emptyMap()));

        // Act - run consumption in separate thread and stop after a short delay
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> consumptionService.startConsumption(edrDataList));
        
        Thread.sleep(100); // Allow some time for consumption to start
        consumptionService.stop();
        
        future.get(1, TimeUnit.SECONDS);

        // Assert
        verify(kafkaConsumer).subscribe(List.of("topic1", "topic2"));
        verify(kafkaConsumer, atLeastOnce()).poll(any(Duration.class));
        verify(messageHandler).accept(record);
        verify(kafkaConsumer).close();
    }

    @Test
    void shouldHandleEmptyTopicsGracefully() {
        // Arrange
        EDRData edrData = createEdrDataWithNullTopic();
        List<EDRData> edrDataList = List.of(edrData);

        // Act
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> consumptionService.startConsumption(edrDataList));
        
        // Allow some time for the service to process
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        consumptionService.stop();

        // Assert - should return early without subscribing
        verify(kafkaConsumer, never()).subscribe(anyList());
        assertThat(future).succeedsWithin(Duration.ofSeconds(1));
    }

    @Test
    void shouldStopConsumptionGracefully() {
        // Arrange
        when(consumerFactory.createConsumer(any())).thenReturn(kafkaConsumer);
        EDRData edrData = createEdrData("test-topic");
        List<EDRData> edrDataList = List.of(edrData);

        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(new ConsumerRecords<>(Collections.emptyMap()));

        // Act
        assertThat(consumptionService.isRunning()).isFalse();
        
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> consumptionService.startConsumption(edrDataList));
        
        // Wait a bit for consumption to start
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        consumptionService.stop();
        
        // Assert
        assertThat(future).succeedsWithin(Duration.ofSeconds(1));
        assertThat(consumptionService.isRunning()).isFalse();
    }

    @Test
    void shouldUseDefaultMessageHandlerWhenNoneProvided() throws Exception {
        // Arrange
        when(consumerFactory.createConsumer(any())).thenReturn(kafkaConsumer);
        consumptionService = new KafkaTopicConsumptionService(consumerFactory);
        EDRData edrData = createEdrData("test-topic");
        List<EDRData> edrDataList = List.of(edrData);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("test-topic", 0, 0L, "key", "value");
        ConsumerRecords<String, String> records = createConsumerRecords(record);

        when(kafkaConsumer.poll(any(Duration.class))).thenReturn(records, new ConsumerRecords<>(Collections.emptyMap()));

        // Act
        CompletableFuture<Void> future = CompletableFuture.runAsync(() -> consumptionService.startConsumption(edrDataList));
        
        Thread.sleep(100);
        consumptionService.stop();
        
        future.get(1, TimeUnit.SECONDS);

        // Assert - should not throw exception and should process the record
        verify(kafkaConsumer).subscribe(List.of("test-topic"));
        verify(kafkaConsumer, atLeastOnce()).poll(any(Duration.class));
    }

    private EDRData createEdrData(String topic) {
        return EDRData.builder()
                .topic(topic)
                .endpoint("localhost:9092")
                .kafkaGroupPrefix("test-group")
                .kafkaSecurityProtocol("SASL_PLAINTEXT")
                .kafkaSaslMechanism("OAUTHBEARER")
                .kafkaPollDuration("PT10S")
                .build();
    }

    private EDRData createEdrDataWithNullTopic() {
        return EDRData.builder()
                .endpoint("localhost:9092")
                .kafkaGroupPrefix("test-group")
                .kafkaSecurityProtocol("SASL_PLAINTEXT")
                .kafkaSaslMechanism("OAUTHBEARER")
                .kafkaPollDuration("PT10S")
                .build();
    }

    private ConsumerRecords<String, String> createConsumerRecords(ConsumerRecord<String, String> record) {
        TopicPartition topicPartition = new TopicPartition(record.topic(), record.partition());
        List<ConsumerRecord<String, String>> recordList = new ArrayList<>();
        recordList.add(record);
        return new ConsumerRecords<>(Collections.singletonMap(topicPartition, recordList));
    }
}