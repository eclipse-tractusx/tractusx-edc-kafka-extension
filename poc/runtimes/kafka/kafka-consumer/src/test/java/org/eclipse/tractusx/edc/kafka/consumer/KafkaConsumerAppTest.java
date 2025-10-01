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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerAppTest {

    @Mock
    private DataTransferClient dataTransferClient;

    @Mock
    private KafkaTopicConsumptionService consumptionService;

    private KafkaConsumerApplication kafkaConsumerApp;

    @BeforeEach
    void setUp() {
        kafkaConsumerApp = new KafkaConsumerApplication();
    }

    @Test
    void shouldRunSuccessfullyWithValidEdrData() throws Exception {
        // Arrange
        EDRData validEdrData = createValidEdrData();
        when(dataTransferClient.executeDataTransferWorkflow(any())).thenReturn(validEdrData);

        // Act
        kafkaConsumerApp.legacyModeRunner(dataTransferClient, consumptionService).run();

        // Assert
        verify(dataTransferClient, times(2)).executeDataTransferWorkflow(any());
        verify(consumptionService).startConsumption(anyList());
    }

    @ParameterizedTest
    @MethodSource("dataFetchExceptionProvider")
    void shouldHandleDataFetchFailures(Exception exception, Class<? extends Exception> expectedCauseType) throws Exception {
        // Arrange
        when(dataTransferClient.executeDataTransferWorkflow(any())).thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> kafkaConsumerApp.legacyModeRunner(dataTransferClient, consumptionService).run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Application failed to start")
                .hasCauseInstanceOf(expectedCauseType);

        verify(dataTransferClient).executeDataTransferWorkflow(any());
        verify(consumptionService, never()).startConsumption(any());
    }

    static Stream<Arguments> dataFetchExceptionProvider() {
        return Stream.of(
                Arguments.of(new IOException("Catalog request failed"), IOException.class),
                Arguments.of(new IOException("Connection failed"), IOException.class),
                Arguments.of(new InterruptedException("Interrupted"), InterruptedException.class)
        );
    }

    @Test
    void shouldThrowRuntimeExceptionWhenConsumptionServiceFails() throws Exception {
        // Arrange
        EDRData validEdrData = createValidEdrData();

        when(dataTransferClient.executeDataTransferWorkflow(any())).thenReturn(validEdrData);
        doThrow(new RuntimeException("Consumption failed")).when(consumptionService).startConsumption(List.of(validEdrData));

        // Act & Assert
        assertThatThrownBy(() -> kafkaConsumerApp.legacyModeRunner(dataTransferClient, consumptionService).run())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Application failed to start")
                .hasCauseInstanceOf(RuntimeException.class);

        verify(dataTransferClient, times(2)).executeDataTransferWorkflow(any());
        verify(consumptionService).startConsumption(anyList());
    }


    private EDRData createValidEdrData() {
        return EDRData.builder()
                .topic("test-topic")
                .endpoint("localhost:9092")
                .kafkaGroupPrefix("test-group")
                .kafkaSecurityProtocol("SASL_PLAINTEXT")
                .kafkaSaslMechanism("OAUTHBEARER")
                .kafkaPollDuration("PT10S")
                .build();
    }
}