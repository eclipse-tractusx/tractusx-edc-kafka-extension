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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.eclipse.tractusx.edc.kafka.producer.KafkaProducerApp.*;

/**
 * This class is responsible for setting up an EDC (Eclipse Dataspace Connector) offer.
 * It facilitates the creation of assets, policy definitions, and contract definitions
 * necessary for the operation of the EDC.
 */
@Slf4j
public class EdcSetup {
    static final String ASSETS_PATH = "/v3/assets";
    static final String POLICY_DEFINITIONS_PATH = "/v3/policydefinitions";
    static final String CONTRACT_DEFINITIONS_PATH = "/v3/contractdefinitions";
    static final String CONTENT_TYPE_JSON = "application/json";

    private final HttpClient client;

    public EdcSetup(final HttpClient client) {
        this.client = client;
    }

    /**
     * Sets up the EDC offer by creating asset, policy and contract definitions
     */
    void setupEdcOffer() {
        log.info("Setting up EDC offer...");
        try {
            createAsset(FORECAST_ASSET_ID,  KAFKA_PRODUCTION_FORECAST_TOPIC);
            createAsset(TRACKING_ASSET_ID,  KAFKA_PRODUCTION_TRACKING_TOPIC);
            // Default AssetID, Default Topic
            createAsset(ASSET_ID, KAFKA_STREAM_TOPIC);
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
        final String assetJson = EdcConfig.getAssetJson(assetId, topic);
        final HttpResponse<String> response = sendJsonRequest(ASSETS_PATH, assetJson);
        log.info("Asset creation response: {} - {}", response.statusCode(), response.body());
    }

    private void createPolicyDefinition() throws IOException, InterruptedException {
        final String policyJson = EdcConfig.getPolicyDefinitionJson();
        final HttpResponse<String> response = sendJsonRequest(POLICY_DEFINITIONS_PATH, policyJson);
        log.info("Policy definition response: {} - {}", response.statusCode(), response.body());
    }

    private void createContractDefinition() throws IOException, InterruptedException {
        final String contractJson = EdcConfig.getContractDefinitionJson();
        final HttpResponse<String> response = sendJsonRequest(CONTRACT_DEFINITIONS_PATH, contractJson);
        log.info("Contract definition response: {} - {}", response.statusCode(), response.body());
    }

    private HttpResponse<String> sendJsonRequest(final String path, final String jsonBody) throws IOException, InterruptedException {
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EDC_MANAGEMENT_URL + path))
                .header("Content-Type", CONTENT_TYPE_JSON)
                .header("X-API-KEY", EDC_API_AUTH_KEY)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
        log.info("Sending request: {}", request);
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Configuration class providing EDC-specific JSON payloads
     */
    private static class EdcConfig {
        private static final String POLICY_ID = "no-constraint-policy";
        private static final String CONTRACT_DEFINITION_ID = "contract-definition";

        static String getAssetJson(final String assetId, final String topic) {
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
                        "kafka.sasl.mechanism": "OAUTHBEARER",
                        "kafka.security.protocol": "SASL_PLAINTEXT",
                        "tokenUrl": "%s",
                        "revokeUrl": "%s",
                        "clientId": "%s",
                        "clientSecretKey": "%s"
                      }
                    }
                    """.formatted(assetId, KAFKA_BOOTSTRAP_SERVERS, topic, KEYCLOAK_TOKEN_URL, KEYCLOAK_REVOKE_URL, KEYCLOAK_CLIENT_ID, VAULT_CLIENT_SECRET_KEY);
        }

        static String getPolicyDefinitionJson() {
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

        static String getContractDefinitionJson() {
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
}