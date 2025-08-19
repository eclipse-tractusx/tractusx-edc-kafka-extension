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
package org.eclipse.tractusx.edc.extensions.kafka.acl;

import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link KafkaAclServiceImpl}.
 * Note: These tests verify the service logic without requiring an actual Kafka cluster.
 * Integration tests with a real Kafka cluster should be implemented separately.
 */
class KafkaAclServiceImplTest {

    private Monitor monitor;

    private KafkaAclServiceImpl aclService;
    private Properties kafkaProperties;

    @BeforeEach
    void setUp() {
        monitor = spy(new ConsoleMonitor());
        
        kafkaProperties = new Properties();
        kafkaProperties.put("bootstrap.servers", "localhost:9092");
        kafkaProperties.put("security.protocol", "PLAINTEXT");
        
        aclService = new KafkaAclServiceImpl(kafkaProperties, monitor);
    }

    @Test
    void createAclsForSubject_shouldLogDebugMessage() {
        // Given
        String oauthSubject = "test-subject";
        String topicName = "test-topic";
        String transferProcessId = "test-process-id";

        // When - This will fail with connection error, but we can verify debug logging
        Result<Void> result = aclService.createAclsForSubject(oauthSubject, topicName, transferProcessId);

        // Then
        assertThat(result.failed()).isTrue();
        verify(monitor).debug("Creating ACLs for OAuth subject: " + oauthSubject + ", topic: " + topicName + 
                             ", transferProcessId: " + transferProcessId);
        verify(monitor).severe(anyString(), org.mockito.ArgumentMatchers.any(Exception.class));
    }

    @Test
    void revokeAclsForTransferProcess_withNonExistentId_shouldSucceed() {
        // Given
        String transferProcessId = "non-existent-id";

        // When
        Result<Void> result = aclService.revokeAclsForTransferProcess(transferProcessId);

        // Then
        assertThat(result.succeeded()).isTrue();
        verify(monitor).debug("Revoking ACLs for transferProcessId: " + transferProcessId);
        verify(monitor).debug("No ACLs found for transferProcessId: " + transferProcessId);
    }

    @Test
    void revokeAclsForSubject_shouldLogDebugMessage() {
        // Given
        String oauthSubject = "test-subject";
        String topicName = "test-topic";

        // When - This will fail with connection error, but we can verify debug logging
        Result<Void> result = aclService.revokeAclsForSubject(oauthSubject, topicName);

        // Then
        assertThat(result.failed()).isTrue();
        verify(monitor).debug("Revoking ACLs for OAuth subject: " + oauthSubject + ", topic: " + topicName);
        verify(monitor).severe(anyString(), org.mockito.ArgumentMatchers.any(Exception.class));
    }

    @Test
    void createAclsForSubject_withInvalidBootstrapServers_shouldFail() {
        // Given
        Properties invalidProps = new Properties();
        invalidProps.put("bootstrap.servers", "invalid:9999");
        invalidProps.put("security.protocol", "PLAINTEXT");
        
        KafkaAclServiceImpl invalidAclService = new KafkaAclServiceImpl(invalidProps, monitor);
        
        String oauthSubject = "test-subject";
        String topicName = "test-topic";
        String transferProcessId = "test-process-id";

        // When
        Result<Void> result = invalidAclService.createAclsForSubject(oauthSubject, topicName, transferProcessId);

        // Then
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to create ACLs for OAuth subject: " + oauthSubject);
        verify(monitor).severe(anyString(), org.mockito.ArgumentMatchers.any(Exception.class));
    }
}