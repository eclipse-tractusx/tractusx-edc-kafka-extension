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

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.CreateAclsResult;
import org.apache.kafka.clients.admin.DeleteAclsResult;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.acl.AclBinding;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KafkaAclServiceImplTest {

    private static final String TEST_OAUTH_SUBJECT = "test-oauth-subject";
    private static final String TEST_TOPIC_NAME = "test-topic";
    private static final String TEST_TRANSFER_PROCESS_ID = "test-transfer-process-id";

    private Monitor monitor;
    private KafkaAclServiceImpl aclService;
    private Properties kafkaProperties;
    private Admin mockAdmin;
    private AdminClientFactory mockAdminClientFactory;

    @BeforeEach
    void setUp() {
        monitor = mock(Monitor.class);
        kafkaProperties = new Properties();
        kafkaProperties.put("bootstrap.servers", "localhost:9092");
        kafkaProperties.put("security.protocol", "PLAINTEXT");

        mockAdmin = mock(Admin.class);
        mockAdminClientFactory = mock(AdminClientFactory.class);

        when(mockAdminClientFactory.createAdmin(any(Properties.class))).thenReturn(mockAdmin);

        aclService = new KafkaAclServiceImpl(kafkaProperties, monitor, mockAdminClientFactory);
    }

    @Test
    void createAclsForSubject_shouldCreateAclsSuccessfully() throws ExecutionException, InterruptedException {
        // Arrange
        CreateAclsResult mockCreateResult = mock(CreateAclsResult.class);
        KafkaFuture<Void> mockFuture = mock(KafkaFuture.class);

        when(mockAdmin.createAcls(anyCollection())).thenReturn(mockCreateResult);
        when(mockCreateResult.all()).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(null);

        // Act
        Result<Void> result = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        // Assert
        assertThat(result.succeeded()).isTrue();
        verify(mockAdminClientFactory).createAdmin(kafkaProperties);
        verify(mockAdmin).createAcls(anyCollection());
        verify(mockCreateResult).all();
        verify(mockFuture).get();
        verify(monitor).debug("Creating ACLs for OAuth subject: " + TEST_OAUTH_SUBJECT + ", topic: " + TEST_TOPIC_NAME +
                ", transferProcessId: " + TEST_TRANSFER_PROCESS_ID);
        verify(monitor).debug("Successfully created ACLs for OAuth subject: " + TEST_OAUTH_SUBJECT +
                ", topic: " + TEST_TOPIC_NAME + ", transferProcessId: " + TEST_TRANSFER_PROCESS_ID);

        // Verify that ACLs are tracked
        assertThat(getTransferProcessAclsCount()).isEqualTo(1);
    }

    @Test
    void createAclsForSubject_shouldHandleExecutionException() throws ExecutionException, InterruptedException {
        // Arrange
        CreateAclsResult mockCreateResult = mock(CreateAclsResult.class);
        KafkaFuture<Void> mockFuture = mock(KafkaFuture.class);

        when(mockAdmin.createAcls(anyCollection())).thenReturn(mockCreateResult);
        when(mockCreateResult.all()).thenReturn(mockFuture);
        when(mockFuture.get()).thenThrow(new ExecutionException("Test exception", new RuntimeException()));

        // Act
        Result<Void> result = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to create ACLs for OAuth subject: " + TEST_OAUTH_SUBJECT);
        verify(monitor).severe(anyString(), any(Exception.class));

        // Verify that ACLs are not tracked when creation fails
        assertThat(getTransferProcessAclsCount()).isEqualTo(0);
    }

    @Test
    void revokeAclsForTransferProcess_shouldRevokeAclsSuccessfully() throws Exception {
        // Arrange - First create ACLs to track
        setupSuccessfulCreateAcls();
        aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        DeleteAclsResult mockDeleteResult = mock(DeleteAclsResult.class);
        KafkaFuture<Collection<AclBinding>> mockDeleteFuture = mock(KafkaFuture.class);

        when(mockAdmin.deleteAcls(anyCollection())).thenReturn(mockDeleteResult);
        when(mockDeleteResult.all()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenReturn(java.util.Collections.emptyList());

        // Act
        Result<Void> result = aclService.revokeAclsForTransferProcess(TEST_TRANSFER_PROCESS_ID);

        // Assert
        assertThat(result.succeeded()).isTrue();
        verify(mockAdminClientFactory, times(2)).createAdmin(kafkaProperties);
        verify(mockAdmin).deleteAcls(anyCollection());
        verify(mockDeleteResult).all();
        verify(mockDeleteFuture).get();
        verify(monitor).debug("Revoking ACLs for transferProcessId: " + TEST_TRANSFER_PROCESS_ID);
        verify(monitor).debug("Successfully revoked ACLs for transferProcessId: " + TEST_TRANSFER_PROCESS_ID);

        // Verify that ACLs are no longer tracked
        assertThat(getTransferProcessAclsCount()).isEqualTo(0);
    }

    @Test
    void revokeAclsForTransferProcess_shouldHandleExecutionException() throws Exception {
        // Arrange
        setupSuccessfulCreateAcls();
        aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        DeleteAclsResult mockDeleteResult = mock(DeleteAclsResult.class);
        KafkaFuture<Collection<AclBinding>> mockDeleteFuture = mock(KafkaFuture.class);

        when(mockAdmin.deleteAcls(anyCollection())).thenReturn(mockDeleteResult);
        when(mockDeleteResult.all()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenThrow(new ExecutionException("Test exception", new RuntimeException()));

        // Act
        Result<Void> result = aclService.revokeAclsForTransferProcess(TEST_TRANSFER_PROCESS_ID);

        // Assert
        assertThat(result.failed()).isTrue();
        assertThat(result.getFailureDetail()).contains("Failed to revoke ACLs for transferProcessId: " + TEST_TRANSFER_PROCESS_ID);
        verify(monitor).severe(anyString(), any(Exception.class));
    }

    @Test
    void revokeAclsForSubject_shouldRevokeAclsSuccessfully() throws ExecutionException, InterruptedException {
        // Arrange
        DeleteAclsResult mockDeleteResult = mock(DeleteAclsResult.class);

        KafkaFuture<Collection<AclBinding>> mockDeleteFuture = mock(KafkaFuture.class);

        when(mockAdmin.deleteAcls(anyCollection())).thenReturn(mockDeleteResult);
        when(mockDeleteResult.all()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenReturn(java.util.Collections.emptyList());

        // Act
        Result<Void> result = aclService.revokeAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME);

        // Assert
        assertThat(result.succeeded()).isTrue();
        verify(mockAdminClientFactory).createAdmin(kafkaProperties);
        verify(mockAdmin).deleteAcls(anyCollection());
        verify(mockDeleteResult).all();
        verify(mockDeleteFuture).get();
        verify(monitor).debug("Revoking ACLs for OAuth subject: " + TEST_OAUTH_SUBJECT + ", topic: " + TEST_TOPIC_NAME);
        verify(monitor).info("Successfully revoked ACLs for OAuth subject: " + TEST_OAUTH_SUBJECT + ", topic: " + TEST_TOPIC_NAME);
    }

    @Test
    void revokeAclsForTransferProcess_withNonExistentId_shouldSucceed() {
        // Arrange
        String nonExistentTransferProcessId = "non-existent-id";

        // Act
        Result<Void> result = aclService.revokeAclsForTransferProcess(nonExistentTransferProcessId);

        // Assert
        assertThat(result.succeeded()).isTrue();
        verify(monitor).debug("Revoking ACLs for transferProcessId: " + nonExistentTransferProcessId);
        verify(monitor).debug("No ACLs found for transferProcessId: " + nonExistentTransferProcessId);

        // Verify no AdminClient operations were performed
        verifyNoInteractions(mockAdmin);
        verify(mockAdminClientFactory, never()).createAdmin(any());
    }

    @Test
    void createAclsForSubject_shouldVerifyCorrectAclBindings() throws ExecutionException, InterruptedException {
        // Arrange
        CreateAclsResult mockCreateResult = mock(CreateAclsResult.class);
        KafkaFuture<Void> mockFuture = mock(KafkaFuture.class);

        when(mockAdmin.createAcls(anyCollection())).thenReturn(mockCreateResult);
        when(mockCreateResult.all()).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(null);

        // Act
        Result<Void> result = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        // Assert
        assertThat(result.succeeded()).isTrue();

        // Verify that createAcls was called with the correct number of ACL bindings
        verify(mockAdmin).createAcls(argThat(aclBindings -> {
            return aclBindings.size() == 3; // Should have 3 ACL bindings (READ topic, DESCRIBE topic, READ group)
        }));
    }

    @Test
    void transferProcessAcls_shouldBeTrackedAndRemovedCorrectly() throws Exception {
        // Arrange
        setupSuccessfulCreateAcls();
        setupSuccessfulDeleteAcls();

        // Act - Create ACLs
        Result<Void> createResult = aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        // Assert - ACLs should be tracked
        assertThat(createResult.succeeded()).isTrue();
        assertThat(getTransferProcessAclsCount()).isEqualTo(1);

        // Act - Revoke ACLs
        Result<Void> revokeResult = aclService.revokeAclsForTransferProcess(TEST_TRANSFER_PROCESS_ID);

        // Assert - ACLs should be removed from tracking
        assertThat(revokeResult.succeeded()).isTrue();
        assertThat(getTransferProcessAclsCount()).isEqualTo(0);
    }

    @Test
    void adminClientFactory_shouldBeCalledWithCorrectProperties() throws Exception {
        // Arrange
        setupSuccessfulCreateAcls();
        setupSuccessfulDeleteAcls();

        // Act - Test create operation
        aclService.createAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME, TEST_TRANSFER_PROCESS_ID);

        // Act - Test revoke operation
        aclService.revokeAclsForTransferProcess(TEST_TRANSFER_PROCESS_ID);

        // Act - Test subject revoke operation
        aclService.revokeAclsForSubject(TEST_OAUTH_SUBJECT, TEST_TOPIC_NAME);

        // Assert - Verify factory was called with correct properties
        verify(mockAdminClientFactory, times(3)).createAdmin(kafkaProperties);
    }

    private void setupSuccessfulCreateAcls() throws ExecutionException, InterruptedException {
        CreateAclsResult mockCreateResult = mock(CreateAclsResult.class);
        KafkaFuture<Void> mockFuture = mock(KafkaFuture.class);

        when(mockAdmin.createAcls(anyCollection())).thenReturn(mockCreateResult);
        when(mockCreateResult.all()).thenReturn(mockFuture);
        when(mockFuture.get()).thenReturn(null);
    }

    private void setupSuccessfulDeleteAcls() throws ExecutionException, InterruptedException {
        DeleteAclsResult mockDeleteResult = mock(DeleteAclsResult.class);
        KafkaFuture<Collection<AclBinding>> mockDeleteFuture = mock(KafkaFuture.class);

        when(mockAdmin.deleteAcls(anyCollection())).thenReturn(mockDeleteResult);
        when(mockDeleteResult.all()).thenReturn(mockDeleteFuture);
        when(mockDeleteFuture.get()).thenReturn(java.util.Collections.emptyList());
    }

    private int getTransferProcessAclsCount() {
        try {
            Field transferProcessAclsField = KafkaAclServiceImpl.class.getDeclaredField("transferProcessAcls");
            transferProcessAclsField.setAccessible(true);
            Map<?, ?> transferProcessAcls = (Map<?, ?>) transferProcessAclsField.get(aclService);
            return transferProcessAcls.size();
        } catch (Exception e) {
            throw new RuntimeException("Failed to access transferProcessAcls field", e);
        }
    }
}