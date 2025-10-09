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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.edc.kafka.producer.config.ProducerProperties;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * This class is responsible for setting up an EDC (Eclipse Dataspace Connector) offer.
 * It facilitates the creation of assets, policy definitions, and contract definitions
 * necessary for the operation of the EDC.
 */
@Slf4j
@RequiredArgsConstructor
public class EdcSetup {
    static final String ASSETS_PATH = "/v3/assets";
    static final String POLICY_DEFINITIONS_PATH = "/v3/policydefinitions";
    static final String CONTRACT_DEFINITIONS_PATH = "/v3/contractdefinitions";
    static final String CONTENT_TYPE_JSON = "application/json";
    private static final String POLICY_ID = "no-constraint-policy";
    private static final String CONTRACT_DEFINITION_ID = "contract-definition";

    private final HttpClient httpClient;
    private final ProducerProperties config;

    /**
     * Sets up the EDC offer by creating asset, policy, and contract definitions
     */
    public void setupEdcOffer() {
        log.info("Setting up EDC offer...");
        try {
            createAsset(config.getForecastAssetId(), config.getProductionForecastTopic());
            createAsset(config.getTrackingAssetId(), config.getProductionTrackingTopic());
            createAsset(config.getAssetId(), config.getStreamTopic());
            createPolicyDefinition();
            createContractDefinition();
        } catch (final IOException e) {
            log.error("I/O error setting up EDC offer: {}", e.getMessage(), e);
        } catch (final InterruptedException e) {
            log.error("Process interrupted while setting up EDC offer: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            log.error("Unexpected error setting up EDC offer: {}", e.getMessage(), e);
        }
    }

    private void createAsset(final String assetId, final String topic) throws IOException, InterruptedException {
        final String assetJson = getAssetJson(assetId, topic);
        final HttpResponse<String> response = sendJsonRequest(ASSETS_PATH, assetJson);
        log.info("Asset creation response: {} - {}", response.statusCode(), response.body());
    }

    private void createPolicyDefinition() throws IOException, InterruptedException {
        final String policyJson = getPolicyDefinitionJson();
        final HttpResponse<String> response = sendJsonRequest(POLICY_DEFINITIONS_PATH, policyJson);
        log.info("Policy definition response: {} - {}", response.statusCode(), response.body());
    }

    private void createContractDefinition() throws IOException, InterruptedException {
        final String contractJson = getContractDefinitionJson();
        final HttpResponse<String> response = sendJsonRequest(CONTRACT_DEFINITIONS_PATH, contractJson);
        log.info("Contract definition response: {} - {}", response.statusCode(), response.body());
    }

    private HttpResponse<String> sendJsonRequest(final String path, final String jsonBody) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder().uri(URI.create(config.getManagementUrl() + path)).header("Content-Type", CONTENT_TYPE_JSON).header("X-API-KEY", config.getApiAuthKey()).POST(HttpRequest.BodyPublishers.ofString(jsonBody)).build();
        log.info("Sending request: {}", request);
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String getAssetJson(final String assetId, final String topic) {
        return """
                {
                  "@context": {
                    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                  },
                  "@id": "%s",
                  "properties": {
                    "name": "test asset",
                    "contenttype": "application/json"
                  },
                  "dataAddress": {
                    "type": "KafkaBroker",
                    "name": "test asset",
                    "kafka.bootstrap.servers": "%s",
                    "topic": "%s",
                    "kafka.poll.duration": "PT10S",
                    "kafka.sasl.mechanism": "OAUTHBEARER",
                    "kafka.security.protocol": "%s",
                    "tokenUrl": "%s",
                    "revokeUrl": "%s",
                    "clientId": "%s",
                    "clientSecretKey": "%s"
                  }
                }
                """.formatted(assetId, config.getBootstrapServers(), topic, config.getSecurityProtocol(), config.getTokenUrl(), config.getRevokeUrl(), config.getClientId(), config.getVaultClientSecretKey());
    }

    private String getPolicyDefinitionJson() {
        return """
                {
                  "@context": {
                    "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
                    "odrl": "http://www.w3.org/ns/odrl/2/"
                  },
                  "@id": "%s",
                  "policy": {
                    "@context": "http://www.w3.org/ns/odrl.jsonld",
                    "@type": "Set",
                    "permission": [],
                    "prohibition": [],
                    "obligation": []
                  }
                }
                """.formatted(POLICY_ID);
    }

    private String getContractDefinitionJson() {
        return """
                {
                  "@context": {
                    "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
                  },
                  "@id": "%s",
                  "accessPolicyId": "%s",
                  "contractPolicyId": "%s",
                  "assetsSelector": []
                }
                """.formatted(CONTRACT_DEFINITION_ID, POLICY_ID, POLICY_ID);
    }
}