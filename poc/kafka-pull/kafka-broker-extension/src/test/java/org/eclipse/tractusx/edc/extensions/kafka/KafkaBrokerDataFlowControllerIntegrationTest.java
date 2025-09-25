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
package org.eclipse.tractusx.edc.extensions.kafka;

import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.monitor.ConsoleMonitor;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.spi.types.domain.transfer.FlowType;
import org.eclipse.edc.spi.types.domain.transfer.TransferType;
import org.eclipse.tractusx.edc.extensions.kafka.acl.KafkaAclService;
import org.eclipse.tractusx.edc.extensions.kafka.acl.KafkaAclServiceImpl;
import org.eclipse.tractusx.edc.extensions.kafka.auth.KafkaOAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;
import static org.eclipse.tractusx.edc.extensions.kafka.KafkaBrokerDataFlowController.KAFKA_DATA_TYPE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Testcontainers
@EnabledIfSystemProperty(named = "testcontainers.enabled", matches = "true", disabledReason = "Testcontainers integration tests are disabled. Set -Dtestcontainers.enabled=true to enable them.")
class KafkaBrokerDataFlowControllerIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:4.0.0")
            .asCompatibleSubstituteFor("confluentinc/cp-kafka"))
            .withEnv("KAFKA_AUTHORIZER_CLASS_NAME", "org.apache.kafka.metadata.authorizer.StandardAuthorizer")
            .withEnv("KAFKA_SUPER_USERS", "User:ANONYMOUS");

    private final Vault vault = mock();
    private final KafkaOAuthService oauthService = mock();
    private final DataFlowPropertiesProvider propertiesProvider = mock();
    private final TransferTypeParser transferTypeParser = mock();

    private KafkaBrokerDataFlowController controller;
    private TransferProcess transferProcess;
    private Policy policy;

    @BeforeEach
    void setUp() {
        DataAddress contentDataAddress = DataAddress.Builder.newInstance()
                .type(KAFKA_TYPE)
                .property(TOPIC, "integration-test-topic")
                .property(BOOTSTRAP_SERVERS, kafka.getBootstrapServers())
                .property(PROTOCOL, "SASL_PLAINTEXT")
                .property(MECHANISM, "OAUTHBEARER")
                .property(GROUP_PREFIX, "integration-test-group")
                .property(POLL_DURATION, "PT5M")
                .property(OAUTH_TOKEN_URL, "http://localhost:8080/token")
                .property(OAUTH_CLIENT_ID, "integration-client")
                .property(OAUTH_CLIENT_SECRET_KEY, "integration-secret-key")
                .property(TOKEN, "integration-token")
                .build();

        transferProcess = TransferProcess.Builder.newInstance()
                .contentDataAddress(contentDataAddress)
                .transferType("KafkaBroker-PULL")
                .contractId("integration-contract")
                .correlationId("integration-correlation")
                .id("integration-transfer-process")
                .build();
        
        policy = Policy.Builder.newInstance().assignee("integration-test-group").build();

        // Mock dependencies
        when(vault.resolveSecret("integration-secret-key")).thenReturn("integration-secret-value");
        when(vault.resolveSecret("test-secret-key")).thenReturn("test-secret-value");
        when(vault.resolveSecret("timeout-secret-key")).thenReturn("timeout-secret-value");
        when(vault.resolveSecret("integration-transfer-process")).thenReturn("integration-oauth-token");
        when(oauthService.getAccessToken(any())).thenReturn(TokenRepresentation.Builder.newInstance().token(createValidJwtToken()).expiresIn(500L).build());
        when(transferTypeParser.parse(any())).thenReturn(Result.success(new TransferType("Kafka", FlowType.PULL)));

        Map<String, Object> properties = contentDataAddress.getProperties();
        when(propertiesProvider.propertiesFor(any(), any())).thenReturn(
                StatusResult.success(properties.entrySet().stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()))));

        Properties aclProperties = new Properties();
        aclProperties.put(KAFKA_BOOTSTRAP_SERVERS_PROPERTY,  kafka.getBootstrapServers());
        KafkaAclService aclService = new KafkaAclServiceImpl(aclProperties, new ConsoleMonitor());

        controller = new KafkaBrokerDataFlowController(vault, oauthService, aclService, transferTypeParser, propertiesProvider);
    }

    @Test
    void shouldStartTransferWithRealKafkaContainer() {
        // Act
        StatusResult<DataFlowResponse> result = controller.start(transferProcess, policy);

        // Assert
        assertThat(result.succeeded()).isTrue();
        assertThat(result.getContent()).isNotNull();
        
        DataAddress resultDataAddress = result.getContent().getDataAddress();
        assertThat(resultDataAddress.getType()).isEqualTo(KAFKA_DATA_TYPE);
        assertThat(resultDataAddress.getStringProperty(EDC_NAMESPACE + "endpoint")).isEqualTo(kafka.getBootstrapServers());
        assertThat(resultDataAddress.getStringProperty(TOPIC)).isEqualTo("integration-test-topic");
        assertThat(resultDataAddress.getStringProperty(GROUP_PREFIX)).isEqualTo("integration-test-group");
    }

    @Test
    void shouldHandleKafkaUnavailability() {
        // Arrange - Create data address with invalid Kafka server
        DataAddress invalidDataAddress = DataAddress.Builder.newInstance()
                .type(KAFKA_TYPE)
                .property(TOPIC, "test-topic")
                .property(BOOTSTRAP_SERVERS, "invalid-kafka:9092")
                .property(PROTOCOL, "SASL_PLAINTEXT")
                .property(MECHANISM, "OAUTHBEARER")
                .property(GROUP_PREFIX, "test-group")
                .property(OAUTH_TOKEN_URL, "http://localhost:8080/token")
                .property(OAUTH_CLIENT_ID, "test-client")
                .property(OAUTH_CLIENT_SECRET_KEY, "test-secret-key")
                .build();

        TransferProcess invalidTransferProcess = TransferProcess.Builder.newInstance()
                .contentDataAddress(invalidDataAddress)
                .transferType("KafkaBroker-PULL")
                .contractId("test-contract")
                .correlationId("test-correlation")
                .id("test-transfer-process")
                .build();

        // Act
        StatusResult<DataFlowResponse> result = controller.start(invalidTransferProcess, policy);

        // Assert - Should still succeed as controller doesn't validate Kafka connectivity at start
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldSuspendTransferSuccessfully() {
        // Act
        StatusResult<?> result = controller.suspend(transferProcess);

        // Assert
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void shouldTerminateTransferSuccessfully() {
        // Act
        StatusResult<?> result = controller.terminate(transferProcess);

        // Assert
        assertThat(result.succeeded()).isTrue();
    }

    private String createValidJwtToken() {
        long now = Instant.now().getEpochSecond();
        long exp = now + 300; // 5 minutes from now

        String header = Base64.getUrlEncoder().withoutPadding().encodeToString(
                """
                {"alg":"HS256","typ":"JWT"}
                """.getBytes()
        );

        String payload = Base64.getUrlEncoder().withoutPadding().encodeToString(
                String.format("""
                {"sub":"test-subject","iat":%d,"exp":%d,"scope":"read write"}
                """, now, exp).getBytes()
        );

        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString("fake-signature".getBytes());

        return header + "." + payload + "." + signature;
    }
}