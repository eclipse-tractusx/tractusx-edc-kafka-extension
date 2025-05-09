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
import org.eclipse.edc.connector.controlplane.transfer.spi.types.DataFlowResponse;
import org.eclipse.edc.connector.controlplane.transfer.spi.types.TransferProcess;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.EdcException;
import org.eclipse.edc.spi.response.ResponseStatus;
import org.eclipse.edc.spi.response.StatusResult;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.types.domain.DataAddress;
import org.eclipse.tractusx.edc.extensions.kafka.auth.KafkaOAuthService;
import org.eclipse.tractusx.edc.extensions.kafka.auth.OAuthCredentials;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

import static org.eclipse.edc.spi.types.domain.edr.EndpointDataReference.*;
import static org.eclipse.tractusx.edc.dataaddress.kafka.spi.KafkaBrokerDataAddressSchema.*;

class KafkaBrokerDataFlowController implements DataFlowController {
    static final String START_FAILED = "Failed to start data flow: ";
    static final String SUSPEND_FAILED = "Failed to suspend data flow: ";
    static final String TERMINATE_FAILED = "Failed to terminate data flow: ";
    static final String SECRET_NOT_DEFINED = "secret key %s was not defined";
    private static final String TRANSFER_TYPE = "Kafka-PULL";
    private final Vault vault;
    private final KafkaOAuthService oauthService;

    public KafkaBrokerDataFlowController(final Vault vault, final KafkaOAuthService oauthService) {
        this.vault = vault;
        this.oauthService = oauthService;
    }

    @Override
    public boolean canHandle(final TransferProcess transferProcess) {
        return KAFKA_TYPE.equals(transferProcess.getContentDataAddress().getType())
                && TRANSFER_TYPE.equals(transferProcess.getTransferType());
    }

    @Override
    public @NotNull StatusResult<DataFlowResponse> start(final TransferProcess transferProcess, final Policy policy) {
        try {
            var contentDataAddress = transferProcess.getContentDataAddress();
            var transferProcessId = transferProcess.getId();
            var topic = contentDataAddress.getStringProperty(TOPIC);
            var endpoint = contentDataAddress.getStringProperty(BOOTSTRAP_SERVERS);
            var contractId = transferProcess.getContractId();
            var mechanism = contentDataAddress.getStringProperty(MECHANISM);
            var protocol = contentDataAddress.getStringProperty(PROTOCOL);
            var defaultPollDuration = Duration.ofSeconds(1);
            var pollDuration = Optional.ofNullable(contentDataAddress.getStringProperty(POLL_DURATION)).map(Duration::parse).orElse(defaultPollDuration);
            var groupPrefix = policy.getAssignee();

            OAuthCredentials creds = extractOAuthCredentials(contentDataAddress);
            var token = oauthService.getAccessToken(creds);

            vault.storeSecret(transferProcessId, token);

            var kafkaDataAddress = DataAddress.Builder.newInstance()
                    .type(EDR_SIMPLE_TYPE)
                    .property(ID, transferProcessId)
                    .property(ENDPOINT, endpoint)
                    .property(TOKEN, token)
                    .property(CONTRACT_ID, contractId)
                    .property(TOPIC, topic)
                    .property(PROTOCOL, protocol)
                    .property(MECHANISM, mechanism)
                    .property(GROUP_PREFIX, groupPrefix)
                    .property(POLL_DURATION, pollDuration)
                    .build();

            return StatusResult.success(DataFlowResponse.Builder.newInstance().dataAddress(kafkaDataAddress).build());
        } catch (EdcException e) {
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, START_FAILED + e.getMessage());
        }
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
            return StatusResult.failure(ResponseStatus.FATAL_ERROR, error + e.getMessage());
        }
    }
}