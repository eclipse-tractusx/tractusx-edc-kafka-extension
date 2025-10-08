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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.eclipse.tractusx.edc.kafka.producer.config.ProducerProperties;
import org.eclipse.tractusx.edc.kafka.producer.messages.Message;
import org.eclipse.tractusx.edc.kafka.producer.messages.MessageLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaProducerServiceTest {

    @Mock
    private KafkaProducer<String, String> mockProducer;

    @Mock
    private MessageLoader mockMessageLoader;

    @Mock
    private ObjectMapper mockObjectMapper;

    @Mock
    private Future<RecordMetadata> mockFuture;

    @Mock
    private RecordMetadata mockRecordMetadata;

    private KafkaProducerService kafkaProducerService;

    @BeforeEach
    void setUp() {
        ProducerProperties config = new ProducerProperties();
        kafkaProducerService = new KafkaProducerService(mockProducer, mockMessageLoader, mockObjectMapper, config);
    }

    @Test
    void shouldSendMessageSuccessfully() throws IOException {
        // Given
        Message testMessage = mock(Message.class);
        String topic = "test-topic";
        String expectedPayload = "{\"test\":\"data\"}";

        when(mockObjectMapper.writeValueAsString(testMessage)).thenReturn(expectedPayload);

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);

        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(mockRecordMetadata, null);
            return mockFuture;
        }).when(mockProducer).send(any(ProducerRecord.class), any(Callback.class));

        when(mockRecordMetadata.partition()).thenReturn(0);
        when(mockRecordMetadata.offset()).thenReturn(123L);

        // When
        kafkaProducerService.sendMessage(testMessage, topic);

        // Then
        verify(mockObjectMapper).writeValueAsString(testMessage);
        verify(mockProducer).send(recordCaptor.capture(), callbackCaptor.capture());

        ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.topic()).isEqualTo(topic);
        assertThat(capturedRecord.value()).isEqualTo(expectedPayload);
        assertThat(capturedRecord.key()).contains("Message").contains("-");
    }

    @Test
    void shouldHandleJsonSerializationError() throws IOException {
        // Given
        Message testMessage = mock(Message.class);
        String topic = "test-topic";
        JsonProcessingException expectedException = new JsonProcessingException("Serialization failed") {};

        when(mockObjectMapper.writeValueAsString(testMessage)).thenThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> kafkaProducerService.sendMessage(testMessage, topic))
                .isInstanceOf(IOException.class)
                .hasMessage("Serialization failed");

        verify(mockProducer, never()).send(any(ProducerRecord.class), any(Callback.class));
    }

    @Test
    void shouldSendMessageWithTopicKeyAndPayload() throws ExecutionException, InterruptedException {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        String message = "test-message";

        ArgumentCaptor<ProducerRecord<String, String>> recordCaptor = ArgumentCaptor.forClass(ProducerRecord.class);
        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);

        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(mockRecordMetadata, null);
            return mockFuture;
        }).when(mockProducer).send(any(ProducerRecord.class), any(Callback.class));

        // When
        CompletableFuture<SendAck<String, String>> future = kafkaProducerService.sendMessage(topic, key, message);

        // Then
        verify(mockProducer).send(recordCaptor.capture(), callbackCaptor.capture());

        ProducerRecord<String, String> capturedRecord = recordCaptor.getValue();
        assertThat(capturedRecord.topic()).isEqualTo(topic);
        assertThat(capturedRecord.key()).isEqualTo(key);
        assertThat(capturedRecord.value()).isEqualTo(message);

        SendAck<String, String> result = future.get();
        assertThat(result.producerRecord()).isEqualTo(capturedRecord);
        assertThat(result.metadata()).isEqualTo(mockRecordMetadata);
    }

    @Test
    void shouldHandleKafkaProducerError() {
        // Given
        String topic = "test-topic";
        String key = "test-key";
        String message = "test-message";
        Exception expectedException = new RuntimeException("Kafka error");

        ArgumentCaptor<Callback> callbackCaptor = ArgumentCaptor.forClass(Callback.class);

        doAnswer(invocation -> {
            Callback callback = invocation.getArgument(1);
            callback.onCompletion(null, expectedException);
            return mockFuture;
        }).when(mockProducer).send(any(ProducerRecord.class), any(Callback.class));

        // When
        CompletableFuture<SendAck<String, String>> future = kafkaProducerService.sendMessage(topic, key, message);

        // Then
        verify(mockProducer).send(any(ProducerRecord.class), callbackCaptor.capture());

        assertThat(future).isCompletedExceptionally();
        assertThatThrownBy(future::get)
                .isInstanceOf(ExecutionException.class)
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("Kafka error");
    }

    @Test
    void shouldHandleMessageLoaderIOException() throws IOException {
        // Given
        IOException expectedException = new IOException("Failed to load messages");
        when(mockMessageLoader.loadForecastMessages()).thenThrow(expectedException);

        // When & Then
        assertThatThrownBy(() -> kafkaProducerService.runProducer())
                .isInstanceOf(IOException.class)
                .hasMessage("Failed to load messages");
    }
}