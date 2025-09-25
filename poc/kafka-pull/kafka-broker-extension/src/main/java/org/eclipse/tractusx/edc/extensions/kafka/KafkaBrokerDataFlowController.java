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

import org.eclipse.edc.connector.controlplane.asset.spi.domain.Asset;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowController;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.DataFlowPropertiesProvider;
import org.eclipse.edc.connector.controlplane.transfer.spi.flow.TransferTypeParser;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.iam.TokenRepresentation;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.extensions.kafka.acl.KafkaAclService;
import org.eclipse.tractusx.edc.extensions.kafka.auth.KafkaOAuthService;
import org.eclipse.tractusx.edc.extensions.kafka.auth.OAuthCredentials;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import java.util.Set;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    public static final String KAFKA_DATA_TYPE = "https://w3id.org/idsa/v4.1/Kafka";
    static final String START_FAILED = "Failed to start data flow: ";
    static final String SUSPEND_FAILED = "Failed to suspend data flow: ";
    static final String TERMINATE_FAILED = "Failed to terminate data flow: ";
    static final String SECRET_NOT_DEFINED = "Secret key %s was not defined";
    private static final String TRANSFER_TYPE = "KafkaBroker-PULL";
    private final Vault vault;
    private final KafkaOAuthService oauthService;
    private final KafkaAclService aclService;
    private final TransferTypeParser transferTypeParser;
    private final DataFlowPropertiesProvider propertiesProvider;

    public KafkaBrokerDataFlowController(final Vault vault, final KafkaOAuthService oauthService,
                                         final KafkaAclService aclService, TransferTypeParser transferTypeParser,
                                         DataFlowPropertiesProvider propertiesProvider) {
        this.vault = vault;
        this.oauthService = oauthService;
        this.aclService = aclService;
        this.transferTypeParser = transferTypeParser;
        this.propertiesProvider = propertiesProvider;
    }

    @Override
    public boolean canHandle(final TransferProcess transferProcess) {
        return KAFKA_TYPE.equals(transferProcess.getContentDataAddress().getType())
                && TRANSFER_TYPE.equals(transferProcess.getTransferType());
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

        TokenRepresentation token;
        try {
            OAuthCredentials creds = extractOAuthCredentials(contentDataAddress);
            token = oauthService.getAccessToken(creds);
        } catch (EdcException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, START_FAILED + e.getMessage());
        }
        vault.storeSecret(transferProcess.getId(), token.getToken());

        // Create ACLs for the OAuth subject
        String oauthSubject = extractOAuthSubject(token);
        String topicName = contentDataAddress.getStringProperty(TOPIC);
        var aclResult = aclService.createAclsForSubject(oauthSubject, topicName, transferProcess.getId());
        if (aclResult.failed()) {
            return StatusResult.failure(FATAL_ERROR, START_FAILED + "Failed to create ACLs: " + aclResult.getFailureDetail());
        }

        var pollDuration = Optional.ofNullable(contentDataAddress.getStringProperty(POLL_DURATION)).orElse(DEFAULT_POLL_DURATION);

        DataAddress dataAddress = buildDataAddress(policy, token, contentDataAddress, pollDuration);
        DataFlowResponse dataFlowResponse = DataFlowResponse.Builder.newInstance()
                .dataAddress(dataAddress)
                .build();

        return StatusResult.success(dataFlowResponse);
    }

    private DataAddress buildDataAddress(Policy policy, TokenRepresentation token, DataAddress contentDataAddress, String pollDuration) {
        return DataAddress.Builder.newInstance()
                .type(KAFKA_DATA_TYPE)
                .property(EDC_NAMESPACE + "endpointType", KAFKA_DATA_TYPE)
                .property(EDC_NAMESPACE + "flowType", "PULL")
                .property(EDC_NAMESPACE + "authorization", token.getToken())
                .property(EDC_NAMESPACE + "transferTypeDestination", "KafkaBroker")
                .property(EDC_NAMESPACE + "endpoint", contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS))
                .property(TOPIC, contentDataAddress.getStringProperty(TOPIC))
                .property(PROTOCOL, contentDataAddress.getStringProperty(PROTOCOL))
                .property(MECHANISM, contentDataAddress.getStringProperty(MECHANISM))
                .property(GROUP_PREFIX, policy.getAssignee())
                .property(POLL_DURATION, pollDuration)
                .property(TX_AUTH_NAMESPACE + "expiresIn", token.getExpiresIn().toString())
                .build();
    }

    private OAuthCredentials extractOAuthCredentials(final DataAddress contentDataAddress) {
        var tokenUrl = contentDataAddress.getStringProperty(OAUTH_TOKEN_URL);
        var clientId = contentDataAddress.getStringProperty(OAUTH_CLIENT_ID);
        var clientSecret = getSecret(contentDataAddress.getStringProperty(OAUTH_CLIENT_SECRET_KEY));

        return new OAuthCredentials(tokenUrl, clientId, clientSecret);
    }

    /**
     * Extracts the OAuth subject (sub claim) from the JWT token.
     * This subject is used as the principal for Kafka ACL creation.
     */
    private String extractOAuthSubject(TokenRepresentation token) {
        try {
            String jwtToken = token.getToken();

            // JWT tokens have three parts separated by dots: header.payload.signature
            String[] parts = jwtToken.split("\\.");
            if (parts.length != 3) {
                throw new EdcException("Invalid JWT token format");
            }

            // Decode the payload (second part)
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));

            // Parse the JSON payload
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode payloadNode = objectMapper.readTree(payload);

            // Extract the subject claim
            JsonNode subNode = payloadNode.get("sub");
            if (subNode == null || subNode.isNull()) {
                throw new EdcException("No 'sub' claim found in JWT token");
            }

            return subNode.asText();

        } catch (Exception e) {
            throw new EdcException("Failed to extract OAuth subject from token: " + e.getMessage(), e);
        }
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
            var transferProcessId = transferProcess.getId();

            // Revoke ACLs for this transfer process
            var aclResult = aclService.revokeAclsForTransferProcess(transferProcessId);
            if (aclResult.failed()) {
                return StatusResult.failure(FATAL_ERROR, error + "Failed to revoke ACLs: " + aclResult.getFailureDetail());
            }

            vault.deleteSecret(transferProcessId);

            return StatusResult.success();

        } catch (EdcException e) {
            return StatusResult.failure(FATAL_ERROR, error + e.getMessage());
        }
    }
}