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

package org.eclipse.tractusx.edc.kafka.producer.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ResourceMessageLoader class that actually test the functionality
 * of loading messages from real resource files on the classpath.
 */
class ResourceMessageLoaderTest {

    private ObjectMapper objectMapper;
    private ResourceMessageLoader loader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        loader = new ResourceMessageLoader(objectMapper);
    }

    @Test
    void loadForecastMessages_SuccessfullyLoadsFromDefaultResource() throws IOException {
        // Act
        List<ForecastMessage> messages = loader.loadForecastMessages();

        // Assert
        assertNotNull(messages);
        assertThat(messages).isNotEmpty();
        assertEquals(10, messages.size()); // Based on the production-forecast.json content

        // Verify the structure of the first message
        ForecastMessage firstMessage = messages.getFirst();
        assertNotNull(firstMessage);
        assertNotNull(firstMessage.getRequest());
        assertNotNull(firstMessage.getHeader());

        // Verify request fields
        ForecastMessage.Request request = firstMessage.getRequest();
        assertNotNull(request.getPrecisionOfForecast());
        assertEquals(12, request.getPrecisionOfForecast().getValue());
        assertEquals("unit:secondUnitOfTime", request.getPrecisionOfForecast().getUnit());
        assertEquals("00000001-0000-0000-C000-000000000001", request.getOrderId());
        assertEquals("BPNL7588787849VQ", request.getCustomerId());
        assertEquals("synchronous", request.getCommunicationMode());
        assertFalse(request.isProductionForecastForAll());

        // Verify header fields
        MessageHeader header = firstMessage.getHeader();
        assertNotNull(header);
        assertEquals("BPNL7588787849VQ", header.getSenderBpn());
        assertEquals("BPNL6666787765VQ", header.getReceiverBpn());
        assertEquals("2.0.0", header.getVersion());
        assertNotNull(header.getMessageId());
        assertNotNull(header.getSentDateTime());
    }

    @Test
    void loadTrackingMessages_SuccessfullyLoadsFromDefaultResource() throws IOException {
        // Act
        List<TrackingMessage> messages = loader.loadTrackingMessages();

        // Assert
        assertNotNull(messages);
        assertThat(messages).isNotEmpty();

        // Verify the structure of the first message
        TrackingMessage firstMessage = messages.getFirst();
        assertNotNull(firstMessage);
        assertNotNull(firstMessage.getRequest());
        assertNotNull(firstMessage.getHeader());

        // Verify request fields
        TrackingMessage.Request request = firstMessage.getRequest();
        assertNotNull(request.getIdentifierNumber());
        assertNotNull(request.getCatenaXId());
        assertNotNull(request.getCustomerId());
        assertNotNull(request.getVersion());
    }

    @Test
    void loadForecastMessages_WithCustomResource_ThrowsExceptionWhenNotFound() {
        // Arrange
        ResourceMessageLoader customLoader = new ResourceMessageLoader(objectMapper, "non-existent-forecast.json", "production-tracking.json");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            customLoader::loadForecastMessages);
        assertEquals("Forecast resource not found on classpath: non-existent-forecast.json", exception.getMessage());
    }

    @Test
    void loadTrackingMessages_WithCustomResource_ThrowsExceptionWhenNotFound() {
        // Arrange
        ResourceMessageLoader customLoader = new ResourceMessageLoader(objectMapper, "production-forecast.json", "non-existent-tracking.json");

        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, 
            customLoader::loadTrackingMessages);
        assertEquals("Tracking resource not found on classpath: non-existent-tracking.json", exception.getMessage());
    }

    @Test
    void loadForecastMessages_AllMessagesHaveValidStructure() throws IOException {
        // Act
        List<ForecastMessage> messages = loader.loadForecastMessages();

        // Assert - Verify all messages have the required structure
        for (int i = 0; i < messages.size(); i++) {
            ForecastMessage message = messages.get(i);
            assertNotNull(message, "Message at index " + i + " should not be null");
            assertNotNull(message.getRequest(), "Request at index " + i + " should not be null");
            assertNotNull(message.getHeader(), "Header at index " + i + " should not be null");
            
            // Verify each message has unique order IDs
            assertNotNull(message.getRequest().getOrderId(), "Order ID at index " + i + " should not be null");
        }

        // Verify all order IDs are unique
        List<String> orderIds = messages.stream()
            .map(msg -> msg.getRequest().getOrderId())
            .toList();
        assertEquals(messages.size(), orderIds.stream().distinct().count(), 
            "All order IDs should be unique");
    }

    @Test
    void loadTrackingMessages_AllMessagesHaveValidStructure() throws IOException {
        // Act
        List<TrackingMessage> messages = loader.loadTrackingMessages();

        // Assert - Verify all messages have the required structure
        for (int i = 0; i < messages.size(); i++) {
            TrackingMessage message = messages.get(i);
            assertNotNull(message, "Message at index " + i + " should not be null");
            assertNotNull(message.getRequest(), "Request at index " + i + " should not be null");
            assertNotNull(message.getHeader(), "Header at index " + i + " should not be null");
        }
    }
}