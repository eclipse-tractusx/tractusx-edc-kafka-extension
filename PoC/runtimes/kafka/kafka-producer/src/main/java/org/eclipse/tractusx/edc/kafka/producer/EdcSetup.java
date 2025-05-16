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

@Slf4j
public class EdcSetup {
    static final String ASSETS_PATH = "/v3/assets";
    static final String POLICY_DEFINITIONS_PATH = "/v3/policydefinitions";
    static final String CONTRACT_DEFINITIONS_PATH = "/v3/contractdefinitions";
    static final String CONTENT_TYPE_JSON = "application/json";

    private final HttpClient client;

    public EdcSetup(HttpClient client) {
        this.client = client;
    }

    /**
     * Sets up the EDC offer by creating asset, policy and contract definitions
     */
    void setupEdcOffer() {
        log.info("Setting up EDC offer...");
        try {
            createAsset();
            createPolicyDefinition();
            createContractDefinition();
        } catch (IOException e) {
            log.error("I/O error setting up EDC offer: {}", e.getMessage(), e);
        } catch (InterruptedException e) {
            log.error("Process interrupted while setting up EDC offer: {}", e.getMessage(), e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("Unexpected error setting up EDC offer: {}", e.getMessage(), e);
        }
    }

    private void createAsset() throws IOException, InterruptedException {
        String assetJson = EdcConfig.getAssetJson();
        HttpResponse<String> response = sendJsonRequest(ASSETS_PATH, assetJson);
        log.info("Asset creation response: {} - {}", response.statusCode(), response.body());
    }

    private void createPolicyDefinition() throws IOException, InterruptedException {
        String policyJson = EdcConfig.getPolicyDefinitionJson();
        HttpResponse<String> response = sendJsonRequest(POLICY_DEFINITIONS_PATH, policyJson);
        log.info("Policy definition response: {} - {}", response.statusCode(), response.body());
    }

    private void createContractDefinition() throws IOException, InterruptedException {
        String contractJson = EdcConfig.getContractDefinitionJson();
        HttpResponse<String> response = sendJsonRequest(CONTRACT_DEFINITIONS_PATH, contractJson);
        log.info("Contract definition response: {} - {}", response.statusCode(), response.body());
    }

    private HttpResponse<String> sendJsonRequest(String path, String jsonBody) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
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

        static String getAssetJson() {
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
                    """.formatted(ASSET_ID, KAFKA_BOOTSTRAP_SERVERS, KAFKA_STREAM_TOPIC, KEYCLOAK_TOKEN_URL, KEYCLOAK_REVOKE_URL, KEYCLOAK_CLIENT_ID, VAULT_CLIENT_SECRET_KEY);
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