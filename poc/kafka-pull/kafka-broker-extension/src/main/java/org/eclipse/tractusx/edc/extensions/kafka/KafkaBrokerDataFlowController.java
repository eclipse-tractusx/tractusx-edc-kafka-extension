/*
 * Copyright (c) 2025 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 Cofinity-X GmbH
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.connector.dataplane.selector.spi.DataPlaneSelectorService;
import org.eclipse.edc.connector.dataplane.selector.spi.client.DataPlaneClientFactory;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.edc.web.spi.configuration.context.ControlApiUrl;
import org.eclipse.tractusx.edc.extensions.kafka.auth.KafkaOAuthService;
import org.eclipse.tractusx.edc.extensions.kafka.auth.OAuthCredentials;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;
import static org.eclipse.edc.spi.response.ResponseStatus.FATAL_ERROR;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;

/**
 * Implementation of the {@link DataFlowController} interface responsible for managing data flows
 * using Kafka.
 * It integrates with OAuth for token management and authorizes data transfer operations.
 */
class KafkaBrokerDataFlowController implements DataFlowController {
    public static final String DEFAULT_POLL_DURATION = Duration.ofSeconds(1).toString();
    public static final String TX_AUTH_NAMESPACE = "https://w3id.org/tractusx/auth/";
    static final String START_FAILED = "Failed to start data flow: ";
    static final String SUSPEND_FAILED = "Failed to suspend data flow: ";
    static final String TERMINATE_FAILED = "Failed to terminate data flow: ";
    static final String SECRET_NOT_DEFINED = "Secret key %s was not defined";
    static final String DESTINATION_TYPE = "HttpData";
    private static final String TRANSFER_TYPE = "KafkaBroker-PULL";
    private static final String TRANSFER_TYPE_PUSH = "KafkaBroker-PUSH";
    private static final String DSPACE_SCHEMA = "https://w3id.org/dspace/v0.8/";
    private final Vault vault;
    private final KafkaOAuthService oauthService;
    private final TransferTypeParser transferTypeParser;
    private final DataFlowPropertiesProvider propertiesProvider;
    private final DataPlaneSelectorService selectorService;
    private final DataPlaneClientFactory clientFactory;
    private final String selectionStrategy;
    private final Monitor monitor;
    private final ControlApiUrl callbackUrl;
    private final ObjectMapper objectMapper;

    public KafkaBrokerDataFlowController(final Vault vault, final KafkaOAuthService oauthService,
                                         TransferTypeParser transferTypeParser, DataFlowPropertiesProvider propertiesProvider, DataPlaneSelectorService selectorService,
                                         DataPlaneClientFactory clientFactory, String selectionStrategy, Monitor monitor, ControlApiUrl callbackUrl) {
        this.vault = vault;
        this.oauthService = oauthService;
        this.transferTypeParser = transferTypeParser;
        this.propertiesProvider = propertiesProvider;
        this.selectorService = selectorService;
        this.clientFactory = clientFactory;
        this.selectionStrategy = selectionStrategy;
        this.monitor = monitor;
        this.callbackUrl = callbackUrl;
        objectMapper = new ObjectMapper();
    }

    @Override
    public boolean canHandle(final TransferProcess transferProcess) {
        return KAFKA_TYPE.equals(transferProcess.getContentDataAddress().getType())
//                && (TRANSFER_TYPE.equals(transferProcess.getTransferType()) || TRANSFER_TYPE_PUSH.equals(transferProcess.getTransferType()))
                ;
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(final TransferProcess transferProcess, final Policy policy) {
        var transferTypeParse = transferTypeParser.parse(transferProcess.getTransferType());
        if (transferTypeParse.failed()) {
            return StatusResult.failure(FATAL_ERROR, transferTypeParse.getFailureDetail());
        }

        var propertiesResult = propertiesProvider.propertiesFor(transferProcess, policy);
        if (propertiesResult.failed()) {
            return StatusResult.failure(FATAL_ERROR, propertiesResult.getFailureDetail());
        }

        var contentDataAddress = transferProcess.getContentDataAddress();

        String token;
        try {
            OAuthCredentials creds = extractOAuthCredentials(contentDataAddress);
            token = oauthService.getAccessToken(creds);
        } catch (EdcException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, START_FAILED + e.getMessage());
        }
        vault.storeSecret(transferProcess.getId(), token);

//        Map<String, String> content = buildProperties(policy, contentDataAddress, propertiesResult);

        monitor.info("TransferProcess infos");
        try {
            monitor.info("TransferProcess: " + objectMapper.writeValueAsString(transferProcess));
            monitor.info("Policy: " + objectMapper.writeValueAsString(policy));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        try {
            monitor.info("Kafka ContentDataAddress: " + objectMapper.writeValueAsString(contentDataAddress));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
//        monitor.info("Kafka new Properties: " + content);
        var pollDuration = Optional.ofNullable(contentDataAddress.getStringProperty(POLL_DURATION)).orElse(DEFAULT_POLL_DURATION);

        DataAddress dataAddress = DataAddress.Builder.newInstance()
//                .properties(new HashMap<>(content))
                .type("https://w3id.org/idsa/v4.1/Kafka")
                .property(EDC_NAMESPACE + "endpointType", "https://w3id.org/idsa/v4.1/Kafka")
                .property(EDC_NAMESPACE + "flowType", "PULL")
                .property(EDC_NAMESPACE + "authorization", token)
                .property(EDC_NAMESPACE + "transferTypeDestination", "KafkaBroker")
                .property(EDC_NAMESPACE + "endpoint", contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS))
                .property(TOPIC, contentDataAddress.getStringProperty(TOPIC))
                .property(PROTOCOL, contentDataAddress.getStringProperty(PROTOCOL))
                .property(MECHANISM, contentDataAddress.getStringProperty(MECHANISM))
                .property(GROUP_PREFIX, policy.getAssignee())
                .property(POLL_DURATION, pollDuration)
                .property(TX_AUTH_NAMESPACE + "expiresIn", "300")
//                .property(TX_AUTH_NAMESPACE + "refreshToken", "")
//                .property(TX_AUTH_NAMESPACE + "refreshEndpoint", "")
//                .property(TX_AUTH_NAMESPACE + "refreshAudience", "")
//                .property(TX_AUTH_NAMESPACE + "audience", contentDataAddress.getStringProperty(TX_AUTH_NAMESPACE + "audience"))
                .build();
        DataFlowResponse dataFlowResponse = DataFlowResponse.Builder.newInstance()
                .dataAddress(dataAddress)
                .build();

        try {
            monitor.info("DataFlow Response: " + objectMapper.writeValueAsString(dataFlowResponse));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return StatusResult.success(dataFlowResponse);
    }

    private @NotNull Map<String, String> buildProperties(Policy policy, DataAddress contentDataAddress, StatusResult<Map<String, String>> propertiesResult) {
        var pollDuration = Optional.ofNullable(contentDataAddress.getStringProperty(POLL_DURATION)).orElse(DEFAULT_POLL_DURATION);
        Map<String, String> content = new HashMap<>(propertiesResult.getContent());
        content.put(TOPIC, contentDataAddress.getStringProperty(TOPIC));
        content.put(PROTOCOL, contentDataAddress.getStringProperty(PROTOCOL));
        content.put(MECHANISM, contentDataAddress.getStringProperty(MECHANISM));
        content.put(GROUP_PREFIX, policy.getAssignee());
        content.put(POLL_DURATION, pollDuration);
        return content;
    }

    private OAuthCredentials extractOAuthCredentials(final DataAddress contentDataAddress) {
        var tokenUrl = contentDataAddress.getStringProperty(OAUTH_TOKEN_URL);
        var revokeUrl = Optional.ofNullable(contentDataAddress.getStringProperty(OAUTH_REVOKE_URL));
        var clientId = contentDataAddress.getStringProperty(OAUTH_CLIENT_ID);
        var clientSecret = getSecret(contentDataAddress.getStringProperty(OAUTH_CLIENT_SECRET_KEY));

        return new OAuthCredentials(tokenUrl, revokeUrl, clientId, clientSecret);
    }

    @Override
    public StatusResult<Void> suspend(final TransferProcess transferProcess) {
        return deleteCredentialsAndRevokeAccess(transferProcess, SUSPEND_FAILED);
    }

    @Override
    public StatusResult<Void> terminate(final TransferProcess transferProcess) {
        return deleteCredentialsAndRevokeAccess(transferProcess, TERMINATE_FAILED);
    }

    @Override
    public Set<String> transferTypesFor(final Asset asset) {
        return Set.of(TRANSFER_TYPE);
    }

    private String getSecret(final String secretKey) {
        return Optional.ofNullable(vault.resolveSecret(secretKey)).orElseThrow(() -> new EdcException(SECRET_NOT_DEFINED.formatted(secretKey)));
    }

    private StatusResult<Void> deleteCredentialsAndRevokeAccess(final TransferProcess transferProcess, final String error) {
        try {
            var contentDataAddress = transferProcess.getContentDataAddress();
            var transferProcessId = transferProcess.getId();

            OAuthCredentials creds = extractOAuthCredentials(contentDataAddress);
            String token = getSecret(transferProcessId);

            oauthService.revokeToken(creds, token);
            vault.deleteSecret(transferProcessId);

            return StatusResult.success();

        } catch (EdcException e) {
            return StatusResult.failure(FATAL_ERROR, error + e.getMessage());
        }
    }
}