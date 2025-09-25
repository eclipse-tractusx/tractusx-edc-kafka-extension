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

import org.eclipse.tractusx.edc.kafka.producer.config.ProducerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EdcSetupTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private EdcSetup edcSetup;

    @BeforeEach
    void setUp() {
        ProducerProperties config = new ProducerProperties();
        edcSetup = new EdcSetup(httpClient, config);
    }

    @Test
    void shouldCreateAssetsSuccessfully() throws Exception {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn("Asset created successfully");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        // Act & Assert
        assertThatCode(() -> edcSetup.setupEdcOffer())
                .doesNotThrowAnyException();

        // 3 assets (forecast, tracking, default) + 1 policy + 1 contract = 5 requests
        verify(httpClient, times(5)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldHandleHttpErrors() throws Exception {
        // Arrange
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpResponse.body()).thenReturn("Internal Server Error");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn(httpResponse);

        // Act & Assert
        assertThatCode(() -> edcSetup.setupEdcOffer())
                .doesNotThrowAnyException();
        verify(httpClient, times(5)).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldHandleEdcServiceUnavailability() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("EDC service unavailable"));

        // Act & Assert
        assertThatCode(() -> edcSetup.setupEdcOffer())
                .doesNotThrowAnyException();
        verify(httpClient, atLeastOnce()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldHandleInterruptedException() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new InterruptedException("Process interrupted"));

        // Act & Assert
        assertThatCode(() -> edcSetup.setupEdcOffer())
                .doesNotThrowAnyException();
        verify(httpClient, atLeastOnce()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }

    @Test
    void shouldHandleUnexpectedExceptions() throws Exception {
        // Arrange
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new RuntimeException("Unexpected error"));

        // Act & Assert
        assertThatCode(() -> edcSetup.setupEdcOffer())
                .doesNotThrowAnyException();
        verify(httpClient, atLeastOnce()).send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class));
    }
}