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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataTransferClientTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private DataTransferClient client;

    @BeforeEach
    void setUp() {
        client = new DataTransferClient("http://localhost:8081/management", "test-provider", "http://localhost:8084/api/v1/dsp", httpClient);
    }

    @ParameterizedTest
    @MethodSource("networkExceptionProvider")
    void shouldHandleNetworkExceptions(Exception exception, Class<? extends Exception> expectedType, String expectedMessage) throws Exception {
        // Arrange
        String assetId = "test-asset";
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(exception);

        // Act & Assert
        assertThatThrownBy(() -> client.executeDataTransferWorkflow(assetId))
                .isInstanceOf(expectedType)
                .hasMessageContaining(expectedMessage);
    }

    static Stream<Arguments> networkExceptionProvider() {
        return Stream.of(
                Arguments.of(new IOException("Connection timeout"), IOException.class, "Connection timeout"),
                Arguments.of(new HttpTimeoutException("Request timeout"), HttpTimeoutException.class, "Request timeout"),
                Arguments.of(new IOException("EDC service unavailable"), IOException.class, "EDC service unavailable"),
                Arguments.of(new IOException("Connection refused"), IOException.class, "Connection refused"),
                Arguments.of(new InterruptedException("Process interrupted"), InterruptedException.class, "Process interrupted")
        );
    }

    @Test
    void shouldHandleInvalidJsonResponse() throws Exception {
        // Arrange
        String assetId = "test-asset";
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("invalid-json");

        // Act & Assert
        assertThatThrownBy(() -> client.executeDataTransferWorkflow(assetId))
                .isInstanceOf(Exception.class); // Accept any exception type since JSON parsing can throw various exceptions
    }

    @ParameterizedTest
    @MethodSource("httpErrorProvider")
    void shouldHandleHttpErrorResponses(int statusCode, String responseBody, String expectedMessage) throws Exception {
        // Arrange
        String assetId = "test-asset";
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(statusCode);
        when(httpResponse.body()).thenReturn(responseBody);

        // Act & Assert
        assertThatThrownBy(() -> client.executeDataTransferWorkflow(assetId))
                .isInstanceOf(IOException.class)
                .hasMessageContaining(expectedMessage);
    }

    static Stream<Arguments> httpErrorProvider() {
        return Stream.of(
                Arguments.of(500, "Internal Server Error", "status code: 500"),
                Arguments.of(429, "Rate limit exceeded", "status code: 429")
        );
    }
}