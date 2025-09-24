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
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.eclipse.tractusx.edc.kafka.producer.config.KafkaConfig;
import org.eclipse.tractusx.edc.kafka.producer.config.ProducerProperties;
import org.eclipse.tractusx.edc.kafka.producer.messages.Message;
import org.eclipse.tractusx.edc.kafka.producer.messages.MessageLoader;
import org.eclipse.tractusx.edc.kafka.producer.messages.TrackingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.apache.kafka.clients.CommonClientConfigs.*;
import static org.apache.kafka.clients.CommonClientConfigs.CONNECTIONS_MAX_IDLE_MS_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.CommonClientConfigs.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.*;
import static org.apache.kafka.clients.producer.ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.producer.ProducerConfig.MAX_BLOCK_MS_CONFIG;
import static org.apache.kafka.common.config.SaslConfigs.*;
import static org.assertj.core.api.Assertions.*;
import static org.eclipse.tractusx.edc.kafka.producer.config.KafkaConfig.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
class KafkaProducerIntegrationTest {

    public static final String NON_EXISTENT_SERVER = "localhost:9999";
    public static final String INVALID_SERVER = "invalid-server:9092";

    public static final String TEST_TOPIC = "test-topic";
    public static final String TEST_KEY = "test-key";
    public static final String TEST_VALUE = "test-value";

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0"));

    private ProducerProperties config;
    private KafkaConfig kafkaConfig;
    private KafkaProducerService producerService;
    private ObjectMapper objectMapper;
    private MessageLoader messageLoader;

    @BeforeEach
    void setUp() {
        config = mock(ProducerProperties.class);
        when(config.getProductionForecastTopic()).thenReturn("ProductionForecastTopic");
        when(config.getProductionTrackingTopic()).thenReturn("ProductionTrackingTopic");
        when(config.getMessageSendIntervalMs()).thenReturn(100L);
        when(config.getBootstrapServers()).thenReturn(kafka.getBootstrapServers());
        when(config.getTokenUrl()).thenReturn("tokenUrl");
        when(config.getClientId()).thenReturn("clientId");
        when(config.getClientSecret()).thenReturn("clientSecret");
        kafkaConfig = new KafkaConfig(config);

        try (AdminClient adminClient = AdminClient.create(basicKafkaProperties())) {
            adminClient.deleteTopics(List.of(TEST_TOPIC));
            adminClient.createTopics(List.of(new NewTopic(TEST_TOPIC, 1, (short) 1)));
        }

        Properties props = basicKafkaProperties();
        KafkaProducer<String, String> producer = kafkaProducer(props);
        objectMapper = objectMapper();
        messageLoader = messageLoader(objectMapper);
        producerService = new KafkaProducerService(producer, messageLoader, objectMapper, config);
    }


    @Test
    void shouldConnectToKafkaAndSendMessage() throws ExecutionException, InterruptedException {
        // Act
        var future = producerService.sendMessage(TEST_TOPIC, TEST_KEY, TEST_VALUE);

        // Assert
        future.get();
        assertThat(future).isCompleted();
        var sendAck = future.get();
        assertThat(sendAck.metadata()).isNotNull();
        assertThat(sendAck.metadata().topic()).isEqualTo(TEST_TOPIC);
        assertThat(sendAck.producerRecord()).isNotNull();
    }

    @Test
    void shouldConnectToKafkaAndSendAndConsumeMessage() {
        // Arrange
        Message message = new TrackingMessage();

        // Act & Assert
        assertThatNoException().isThrownBy(() -> producerService.sendMessage(message, TEST_TOPIC));
    }

    @Test
    void shouldHandleKafkaUnavailability() {
        // Arrange
        when(config.getBootstrapServers()).thenReturn(NON_EXISTENT_SERVER);
        Properties props = basicKafkaProperties();
        KafkaProducer<String, String> producer = kafkaProducer(props);
        KafkaProducerService service = new KafkaProducerService(producer, messageLoader, objectMapper, config);

        // Act
        var future = service.sendMessage(TEST_TOPIC, TEST_KEY, TEST_VALUE);

        // Assert
        assertThat(future).isCompletedExceptionally();
    }

    @Test
    void shouldHandleInvalidBootstrapServers() {
        // Arrange
        when(config.getBootstrapServers()).thenReturn(INVALID_SERVER);
        Properties props = basicKafkaProperties();

        // Act & Assert
        assertThatThrownBy(() -> kafkaProducer(props)).isInstanceOfAny(KafkaException.class);
    }

    @Test
    void shouldSuccessfullyProduceAndConsumeMessage() throws Exception {
        // Arrange
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(basicKafkaConsunmerProperties())) {
            // Act
            SendAck<String, String> ack = producerService.sendMessage(TEST_TOPIC, TEST_KEY, TEST_VALUE).get();

            consumer.subscribe(Collections.singletonList(TEST_TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

            // Assert
            RecordMetadata metadata = ack.metadata();
            assertThat(metadata).isNotNull();
            assertThat(metadata.topic()).isEqualTo(TEST_TOPIC);

            assertThat(records).isNotEmpty();
            assertThat(records.count()).isEqualTo(1);

            ConsumerRecord<String, String> receivedRecord = records.iterator().next();
            assertThat(receivedRecord.topic()).isEqualTo(TEST_TOPIC);
            assertThat(receivedRecord.key()).isEqualTo(TEST_KEY);
            assertThat(receivedRecord.value()).isEqualTo(TEST_VALUE);
        }
    }

    @Test
    void shouldHandleMultipleMessages() throws Exception {
        // Arrange
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(basicKafkaConsunmerProperties())) {
            // Act
            int messageCount = 3;
            for (int i = 0; i < messageCount; i++) {
                producerService.sendMessage(TEST_TOPIC, TEST_KEY + i, TEST_VALUE + i).get();
            }
            consumer.subscribe(Collections.singletonList(TEST_TOPIC));
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));

            // Assert
            assertThat(records.count()).isEqualTo(messageCount);

            int messageIndex = 0;
            for (ConsumerRecord<String, String> consumerRecord : records) {
                assertThat(consumerRecord.topic()).isEqualTo(TEST_TOPIC);
                assertThat(consumerRecord.key()).isEqualTo(TEST_KEY + messageIndex);
                assertThat(consumerRecord.value()).startsWith(TEST_VALUE + messageIndex);
                messageIndex++;
            }
        }
    }

    private Properties basicKafkaProperties() {
        Properties props = kafkaConfig.kafkaProperties();
        props.put(REQUEST_TIMEOUT_MS_CONFIG, "200");
        props.put(DELIVERY_TIMEOUT_MS_CONFIG, "400");
        props.put(CONNECTIONS_MAX_IDLE_MS_CONFIG, "240");
        props.put(MAX_BLOCK_MS_CONFIG, "200");

        // Remove OAuth2 properties
        props.remove(SECURITY_PROTOCOL_CONFIG);
        props.remove(SASL_MECHANISM);
        props.remove(SASL_OAUTHBEARER_TOKEN_ENDPOINT_URL);
        props.remove(SASL_LOGIN_CALLBACK_HANDLER_CLASS);
        props.remove(SASL_JAAS_CONFIG);
        return props;
    }

    private Properties basicKafkaConsunmerProperties() {
        Properties props = basicKafkaProperties();
        props.put(GROUP_ID_CONFIG, "data-flow-test-group");
        props.put(AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        props.put(AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }
}